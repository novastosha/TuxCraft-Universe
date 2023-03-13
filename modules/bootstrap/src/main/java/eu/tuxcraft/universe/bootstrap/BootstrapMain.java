package eu.tuxcraft.universe.bootstrap;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.nova.betterarguments.arguments.Argument;
import dev.nova.betterarguments.parser.ArgumentParser;
import eu.tuxcraft.universe.api.UniverseServer;
import eu.tuxcraft.universe.storage.MemoryServerStorage;
import eu.tuxcraft.universe.storage.MongoServerStorage;
import lombok.experimental.UtilityClass;
import net.minestom.server.MinecraftServer;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Objects;
import java.util.UUID;
import java.util.jar.JarFile;

@UtilityClass
public class BootstrapMain {

    @SuppressWarnings("UnstableApiUsage")
    public static void main(String[] args) {
        final long msStartTime = System.currentTimeMillis();

        System.setProperty("minestom.terminal.disabled", "true");

        var parser = new ArgumentParser(args);

        Argument.Builder.create("development")
                .withAliases("dev")
                .withDescription("Enable development mode")
                .build(parser);

        var portArg = Argument.Builder.create("port", int.class)
                .withDescription("The port of the server")
                .build(parser);

        var jarArg = Argument.Builder.create("jar", File.class)
                .withDescription("The server's JAR file to bootstrap")
                .withAliases("file")
                .withValueConverter(File::new)
                .build(parser);

        var addressArg = Argument.Builder.create("address", String.class)
                .withDescription("The address used for the server")
                .build(parser);

        parser.parse();


        var port = parser.hasArgument("port") ? parser.get(portArg) : 25565;

        var jarFile = parser.hasArgument("jar") ? parser.get(jarArg) : new File("./data/server.jar");
        if (!jarFile.exists()) throw new RuntimeException("Couldn't find the JAR file!");

        var address = parser.hasArgument("address") ? parser.get(addressArg) : "localhost";

        var developmentMode = parser.hasArgument("development");
        var logger = System.getLogger("Bootstrap");


        logger.log(System.Logger.Level.INFO, "Bootstrapping server...");
        logger.log(System.Logger.Level.INFO, "Development mode is " + (developmentMode ? "enabled" : "disabled"));

        logger.log(System.Logger.Level.INFO, "Connecting to server storage...");
        final var serverStorage = developmentMode ? new MemoryServerStorage() : new MongoServerStorage();

        try (var jar = new JarFile(jarFile)) {
            final var universeDataEntry = jar.getEntry("universe.json");
            JsonObject universeData;

            var reader = new InputStreamReader(jar.getInputStream(universeDataEntry));
            if (JsonParser.parseReader(reader) instanceof JsonObject jObj) universeData = jObj;
            else throw new RuntimeException("universe.json is not a JSON object!");

            try {
                var classLoader = loadJARClasses(jar, jarFile.getPath());

                try {
                    var clazz = Class.forName(universeData.get("class").getAsString(), false, classLoader).asSubclass(UniverseServer.class);

                    var constructor = clazz.getConstructor();
                    constructor.setAccessible(true);

                    var server = constructor.newInstance();
                    Runtime.getRuntime().addShutdownHook(new Thread("bootstrap-shutdown-hook") {
                        @Override
                        public void run() {
                            //TODO Delete RabbitMQ queue
                            server.onStop();
                        }
                    });

                    var name = Objects.requireNonNullElse(server.getName(), universeData.get("name").getAsString());
                    var namespace = NamespaceID.from("tuxcraft", name);

                    var dimension = DimensionType.builder(namespace)
                            .ambientLight(2f)
                            .build();

                    MinecraftServer.getDimensionTypeManager().addDimension(dimension);

                    var serverId = UUID.randomUUID();
                    assignServerID(server, serverId);

                    var minestomServer = MinecraftServer.init();

                    minestomServer.start(address, port);

                    //Hook the hooks
                    server.getHooks().values()
                            .forEach(serverHook -> serverHook.hook(MinecraftServer.process()));

                    server.onStart(MinecraftServer.process());
                    logger.log(System.Logger.Level.INFO, "The ID of this server instance is: {}", serverId.toString());

                    // Add the server to database and register RabbitMQ queue
                } catch (ClassCastException exception) {
                    logger.log(System.Logger.Level.ERROR, "Class is not of type: " + UniverseServer.class.getSimpleName(), exception);
                    System.exit(255);
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException(e);
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    logger.log(System.Logger.Level.ERROR, "Couldn't instance server class!", e);
                    System.exit(255);
                }
            } catch (ClassNotFoundException exception) {
                logger.log(System.Logger.Level.ERROR, "Couldn't find class!", exception);
                System.exit(255);
            }

        } catch (IOException exception) {
            logger.log(System.Logger.Level.ERROR, "Couldn't load JAR File!", exception);
            System.exit(255); //Return a status code :)
        }

        final long msEndTime = System.currentTimeMillis();
        logger.log(System.Logger.Level.INFO, "Server started in {0}ms", (msEndTime - msStartTime));
    }

    private static void assignServerID(UniverseServer server, UUID id) throws IllegalAccessException {
        try {
            var idField = server.getClass().getField("id");

            idField.setAccessible(true);
            idField.set(server, id);
        } catch (NoSuchFieldException ignored) {
        } //it exists, alright?
    }

    private static ClassLoader loadJARClasses(JarFile jarFile, String path) throws ClassNotFoundException, MalformedURLException {
        var enumeration = jarFile.entries();

        URL[] urls = {new URL("jar:file:" + path + "!/")};
        var classLoader = URLClassLoader.newInstance(urls, ClassLoader.getSystemClassLoader());

        while (enumeration.hasMoreElements()) {
            var je = enumeration.nextElement();
            if (je.isDirectory() || !je.getName().endsWith(".class")) {
                continue;
            }

            var className = je.getName().substring(0, je.getName().length() - 6);
            className = className.replace('/', '.');
            classLoader.loadClass(className);
        }

        return classLoader;
    }
}

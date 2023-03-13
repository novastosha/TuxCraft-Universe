package eu.tuxcraft.universe.api;

import eu.tuxcraft.universe.api.hooks.Hook;
import eu.tuxcraft.universe.api.hooks.ServerHook;
import net.minestom.server.ServerProcess;
import org.jetbrains.annotations.UnknownNullability;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public abstract class UniverseServer {

    private final System.Logger logger;
    private final Map<Class<? extends ServerHook>, ServerHook> hooks;

    /**
     * This field will be assigned by the bootstrap!
     */
    private UUID id;

    public UniverseServer() {
        this.logger = System.getLogger("Universe Server");
        this.hooks = new HashMap<>();

        Arrays.stream(this.getClass().getAnnotationsByType(Hook.class))
                .forEach(annotation -> {
                    Constructor<? extends ServerHook> constructor;
                    try {
                        constructor = annotation.value().getDeclaredConstructor();
                    } catch (NoSuchMethodException exception) {
                        logger.log(System.Logger.Level.ERROR,
                                "Couldn't find a no-args constructor in class: " + annotation.value().getSimpleName(), exception);
                        return;
                    }

                    constructor.setAccessible(true);
                    try {
                        var instance = constructor.newInstance();
                        hooks.put(annotation.value(), instance);

                        logger.log(System.Logger.Level.INFO, "Registered hook: {}", annotation.value().getSimpleName());
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException exception) {
                        logger.log(System.Logger.Level.ERROR,
                                "Couldn't create instance of hook: " + annotation.value().getSimpleName(), exception);
                    }
                });
    }

    protected final <T extends ServerHook> T getHook(Class<T> clazz) {
        assert hooks.containsKey(clazz) : "This hook doesn't exist!";

        //noinspection unchecked
        return (T) hooks.get(clazz);
    }

    public void onStart(ServerProcess process) {}
    public synchronized void onStop() {}

    public String getName() {
        return null;
    }

    public final Map<Class<? extends ServerHook>, ServerHook> getHooks() {
        return Collections.unmodifiableMap(hooks);
    }

    /**
     *
     * @return the ID of the server
     */
    @UnknownNullability
    public final UUID getId() {
        return id;
    }
}

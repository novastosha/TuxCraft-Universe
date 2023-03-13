package eu.tuxcraft.universe.api.hooks;

import java.lang.annotation.*;

/**
 * Classes implementing {@link eu.tuxcraft.universe.api.UniverseServer} should use this annotation to declare a hook.
 *
 * @see ServerHook
 */
@Repeatable(Hook.ServerHooks.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Hook {

    Class<? extends ServerHook> value();

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface ServerHooks {
        Hook[] value();
    }
}

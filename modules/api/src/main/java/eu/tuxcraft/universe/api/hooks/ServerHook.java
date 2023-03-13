package eu.tuxcraft.universe.api.hooks;

import net.minestom.server.ServerProcess;

/**
 *
 * @implNote <ul>
 *      <li> Any logic related to Minestom should <b>not</b> be done on the constructor!
 *      <li> Hooks are expected to only have a public no-args constructor
 *          </ul>
 *
 */
public interface ServerHook {
    void hook(ServerProcess process);
}

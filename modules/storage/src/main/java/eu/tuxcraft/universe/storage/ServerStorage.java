package eu.tuxcraft.universe.storage;

public sealed interface ServerStorage permits MemoryServerStorage, MongoServerStorage {

    /**
     *  {@inheritDoc}
     *
     * Adds an alive server instance to the database.
     */
    void insertServer();

    /**
     * {@inheritDoc}
     *
     * Removes the server from the database and kills its process
     */
    void removeServer();

    //TODO Add a check method for servers that returns a ServerCheckResult

}

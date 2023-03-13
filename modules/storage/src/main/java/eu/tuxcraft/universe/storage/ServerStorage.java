package eu.tuxcraft.universe.storage;

public sealed interface ServerStorage permits MemoryServerStorage, MongoServerStorage {

    void insertServer()

}

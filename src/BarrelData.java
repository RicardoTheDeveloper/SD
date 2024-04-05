package src;

/**
 * BarrelData is a simple container class that holds the identifier and the stub of an IndexStorageBarrel.
 * It is used to manage the references to the different barrels in the distributed system, allowing
 * the Gateway to communicate with the individual barrels using their stubs.
 */
class BarrelData {
    
    /** The unique identifier for the IndexStorageBarrel. */
    String id;

    /** The remote reference to the IndexStorageBarrel interface, acting as a stub for RMI communication. */
    IndexStorageBarrelInterface stub;
    
    /**
     * Constructs a new BarrelData object with the given ID and stub.
     * 
     * @param id The unique identifier for the IndexStorageBarrel.
     * @param stub The remote stub for the IndexStorageBarrel interface.
     */
    BarrelData(String id, IndexStorageBarrelInterface stub) {
        this.id = id;
        this.stub = stub;
    }
}
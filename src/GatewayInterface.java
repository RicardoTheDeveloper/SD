package src;

import java.rmi.*;

/**
 * Remote interface for the Gateway server in a distributed system.
 * This interface defines the methods that can be remotely invoked by client applications or other components in the system.
 */
public interface GatewayInterface extends Remote {

    /**
     * Submits a URL to the Gateway server for processing.
     * 
     * @param url The URL to be submitted.
     * @return A string message indicating the result of the operation.
     * @throws RemoteException If a remote invocation error occurs.
     */
    String remoteSubmitURL(String url) throws RemoteException;

    /**
     * Fetches the next URL from the queue for processing.
     * 
     * @return The next URL in the queue, or null if the queue is empty.
     * @throws RemoteException If a remote invocation error occurs.
     */
    String fetchNextUrl() throws RemoteException;

    /**
     * Searches for web pages that contain the given terms.
     * 
     * @param terms An array of search terms.
     * @return A string containing search results.
     * @throws RemoteException If a remote invocation error occurs.
     */
    String remoteSearchForTerms(String[] terms) throws RemoteException;

    /**
     * Retrieves the list of inbound links pointing to a specific URL.
     * 
     * @param url The URL for which inbound links are requested.
     * @return A string containing the list of inbound links.
     * @throws RemoteException If a remote invocation error occurs.
     */
    String remoteGetLinksToPointingTo(String url) throws RemoteException;

    /**
     * Blocks until a URL is available in the queue.
     * 
     * @throws InterruptedException If the waiting thread is interrupted.
     * @throws RemoteException If a remote invocation error occurs.
     */
    void waitForUrl() throws InterruptedException, RemoteException;

    /**
     * Notifies the system that new URLs are available in the queue.
     * 
     * @throws RemoteException If a remote invocation error occurs.
     */
    void notifyDownloader() throws RemoteException;

    /**
     * Adds a URL to the processing queue.
     * 
     * @param url The URL to add to the queue.
     * @throws RemoteException If a remote invocation error occurs.
     */
    void addUrlToQueue(String url) throws RemoteException;

    /**
     * Retrieves the current status of the system.
     * 
     * @return A string describing the current system status.
     * @throws RemoteException If a remote invocation error occurs.
     */
    String getSystemStatus() throws RemoteException;

    /**
     * Notifies the Gateway that a specific Index Storage Barrel is now inactive.
     * 
     * @param barrelID The ID of the barrel that has become inactive.
     * @throws RemoteException If a remote invocation error occurs.
     */
    void notifyBarrelInactive(String barrelID) throws RemoteException;



    public void markUrlAsProcessed(String url) throws RemoteException;
}
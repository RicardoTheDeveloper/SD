package src;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The IndexStorageBarrelInterface interface represents the remote interface for an index storage barrel.
 * It defines the methods that can be invoked remotely on an index storage barrel object.
 */
public interface IndexStorageBarrelInterface extends Remote {
    
    /**
     * Searches for pages containing the given terms.
     * 
     * @param terms an array of terms to search for
     * @return a string representing the search results
     * @throws RemoteException if a remote communication error occurs
     */
    String searchPagesWithTerms(String[] terms) throws RemoteException;
    
    /**
     * Retrieves the inbound links for a given URL.
     * 
     * @param url the URL for which to retrieve the inbound links
     * @return a string representing the inbound links
     * @throws RemoteException if a remote communication error occurs
     */
    public String getInboundLinks(String url) throws RemoteException;
    
    /**
     * Retrieves the search frequencies for each term.
     * 
     * @return a map containing the search frequencies for each term
     * @throws RemoteException if a remote communication error occurs
     */
    Map<String, Integer> getSearchFrequencies() throws RemoteException;
    
    /**
     * Retrieves the average response time of the index storage barrel.
     * 
     * @return the average response time
     * @throws RemoteException if a remote communication error occurs
     */
    double getAverageResponseTime() throws RemoteException;
    
    /**
     * Retrieves the page information map of the index storage barrel.
     * 
     * @return a ConcurrentHashMap containing the page information map
     * @throws RemoteException if a remote communication error occurs
     */
    ConcurrentHashMap<String, PageInfo> getPageInfoMap() throws RemoteException;
    
    /**
     * Retrieves the inverted index of the index storage barrel.
     * 
     * @return a ConcurrentHashMap containing the inverted index
     * @throws RemoteException if a remote communication error occurs
     */
    ConcurrentHashMap<String, Set<String>> getInvertedIndex() throws RemoteException;
}

package src;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public interface IndexStorageBarrelInterface extends Remote {
    String searchPagesWithTerms(String[] terms) throws RemoteException;
    public String getInboundLinks(String url) throws RemoteException;
    Map<String, Integer> getSearchFrequencies() throws RemoteException;
    double getAverageResponseTime() throws RemoteException;
    ConcurrentHashMap<String, PageInfo> getPageInfoMap() throws RemoteException;
    ConcurrentHashMap<String, Set<String>> getInvertedIndex() throws RemoteException;
}

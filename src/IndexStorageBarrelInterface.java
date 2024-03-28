package src;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface IndexStorageBarrelInterface extends Remote {
    String searchPagesWithTerms(String[] terms) throws RemoteException;
    public String getInboundLinks(String url) throws RemoteException;
    Map<String, Integer> getSearchFrequencies() throws RemoteException;
    double getAverageResponseTime() throws RemoteException;
}

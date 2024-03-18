package src;

import java.rmi.*;

public interface GatewayInterface extends Remote {
    void remoteSubmitURL(String url) throws  java.rmi.RemoteException;
    public String fetchNextUrl() throws java.rmi.RemoteException;

    void remoteRecursiveIndexUrls() throws RemoteException;
    String remoteSearchForTerms(String[] terms) throws RemoteException;
    String remoteSearchForTermsOrdered(String[] terms) throws RemoteException;
    String remoteGetLinksToPointingTo(String url) throws RemoteException;
    String remoteGetRealTimeSystemStatus() throws RemoteException;
    String remoteCheckInvertedIndex() throws RemoteException;
    public void waitForUrl() throws InterruptedException, RemoteException;
    public void notifyDownloader() throws RemoteException;
    
    
}
import java.rmi.*;

public interface GatewayInterface extends Remote {
    void remoteSubmitURL(String url) throws  java.rmi.RemoteException;
    public String fetchNextUrl() throws java.rmi.RemoteException;
    
}
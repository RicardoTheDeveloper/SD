package src;

import java.rmi.*;

public interface GatewayInterface extends Remote {
    void remoteSubmitURL(String url) throws  java.rmi.RemoteException;
    
}
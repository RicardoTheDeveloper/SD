package src;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IndexingInterface extends Remote {
    void submitURL(String url) throws RemoteException;
    // ... any other remote methods
}
package src;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IndexStorageBarrelInterface extends Remote {
    String searchPagesWithTerms(String[] terms) throws RemoteException;
}

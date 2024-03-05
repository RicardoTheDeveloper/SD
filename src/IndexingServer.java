package src;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;

public class IndexingServer extends UnicastRemoteObject implements IndexingInterface {

    // Constructor
    public IndexingServer() throws RemoteException {
        super(); // Call the constructor of UnicastRemoteObject
        
    }

    // Implementation of the submitURL method
    @Override
    public void submitURL(String url) throws RemoteException {
        // Implementation code goes here
        // For example, add the URL to a queue for the Downloader to process
        System.out.println("URL received for indexing: " + url);
        
        // You can add the logic to pass this URL to your downloader/crawler component here.
        // This could involve adding the URL to a queue, and the Downloader would then take URLs from this queue to process.
    }

    // ... include other remote methods if there are any ...
}

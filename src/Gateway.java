package src;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Gateway extends UnicastRemoteObject implements GatewayInterface {

    private ConcurrentLinkedQueue<String> urlQueue; // List of RMI URLs for Storage Barrels

    protected Gateway() throws RemoteException {
        super();
        urlQueue = new ConcurrentLinkedQueue<>();
    }

   

    /*
     * 
     * public String search(String query) throws RemoteException {
        // Implement logic to select a Storage Barrel
        String barrelUrl = selectStorageBarrel();
        // Connect to the selected Storage Barrel and forward the search query
        // Implement the RMI call to the selected Storage Barrel
        return "Result from Storage Barrel";
    }
     */
    

    @Override
    public void remoteSubmitURL(String url) throws RemoteException {
        // Logic to submit the URL to a Storage Barrel or indexing service
        // This could involve selecting a Storage Barrel as before
        //String barrelUrl = selectStorageBarrel();
        // Assuming there's a method in the Storage Barrel to handle URL submission
        // You would need to implement the RMI call to that method here
        System.out.println("URL submitted to Storage Barrel: " + url);
    }

    /*
     * 
     * private String selectStorageBarrel() {
        // Simple random selection, replace with sophisticated load balancing if needed
        Random random = new Random();
        return storageBarrels.get(random.nextInt(storageBarrels.size()));
    }
     */
    

    public static void main(String[] args) {
        try {
            Gateway server = new Gateway();
            Registry registry = LocateRegistry.createRegistry(1099); // Default RMI registry port
            registry.rebind("GatewayService", server);
            System.out.println("Gateway is ready.");
        } catch (Exception e) {
            System.err.println("Gateway exception: " + e.toString());
            e.printStackTrace();
        }
    }
}


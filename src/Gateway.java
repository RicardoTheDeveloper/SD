package src;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Gateway extends UnicastRemoteObject implements GatewayInterface {

    private ConcurrentLinkedQueue<String> urlQueue; // Queue of URLs to be indexed

    protected Gateway() throws RemoteException {
        super();
        urlQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void remoteSubmitURL(String url) throws RemoteException {
        // Enqueue the URL to the queue
        boolean isAdded = urlQueue.offer(url);
        if (isAdded) {
            System.out.println("URL enqueued: " + url);
        } else {
            // Log or handle the unlikely case where the URL could not be enqueued
            System.err.println("Failed to enqueue URL: " + url);
        }
    }

    @Override
    public String fetchNextUrl() throws RemoteException {
        return urlQueue.poll(); // Retrieves and removes the head of the queue, or returns null if the queue is empty
    }

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

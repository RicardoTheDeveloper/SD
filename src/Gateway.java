
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Gateway extends UnicastRemoteObject implements GatewayInterface {

    private ConcurrentLinkedQueue<String> urlQueue; // Queue de URLs

    protected Gateway() throws RemoteException {
        super();
        urlQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void remoteSubmitURL(String url) throws RemoteException {
        // Meter o URL na queue
        boolean isAdded = urlQueue.offer(url);
        if (isAdded) {
            System.out.println("URL enqueued: " + url);
        } else {
            // print de debugging
            System.err.println("Failed to enqueue URL: " + url);
        }
    }

    @Override
    public String fetchNextUrl() throws RemoteException {
        return urlQueue.poll(); // Retirar um elemento da head da queue
    }

    public static void main(String[] args) {
        try {
            Gateway server = new Gateway();
            Registry registry = LocateRegistry.createRegistry(1099); // porto standard RMI registry
            registry.rebind("GatewayService", server);
            System.out.println("Gateway is ready.");
        } catch (Exception e) {
            System.err.println("Gateway exception: " + e.toString());
            e.printStackTrace();
        }
    }

  
}

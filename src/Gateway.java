package src;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Gateway extends UnicastRemoteObject implements GatewayInterface {

    private ConcurrentLinkedQueue<String> urlQueue; // Queue de URLs

    protected Gateway() throws RemoteException {
        super();
        urlQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public synchronized void remoteSubmitURL(String url) throws RemoteException {
        // Meter o URL na queue
        boolean isAdded = urlQueue.offer(url);
        if (isAdded) {
            notifyDownloader(); // Notify downloader threads
            System.out.println("URL enqueued: " + url);
            System.out.println("DEPOIS DE ADICIONAR");
            printQueueContent();

        } else {
            // print de debugging
            System.err.println("Failed to enqueue URL: " + url);
        }
    }

    @Override
    public synchronized String fetchNextUrl() throws RemoteException {
        synchronized(urlQueue) {
            String url = urlQueue.poll();
            System.out.println("DEPOIS DE DAR FETCH");
            printQueueContent();
            return url; // Retrieve and remove the head of the queue
        }
    }

    @Override
    public void remoteRecursiveIndexUrls() throws RemoteException {
        // Logic for recursive indexing
        System.out.println("Recursive indexing requested.");
    }

    @Override
    public String remoteSearchForTerms(String[] terms) throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1100);
            IndexStorageBarrelInterface barrelStub = (IndexStorageBarrelInterface) registry.lookup("IndexStorageBarrelService");

            return barrelStub.searchPagesWithTerms(terms);
        } catch (Exception e) {
            System.err.println("Error in remoteSearchForTerms: " + e.getMessage());
            return "Search failed.";
        }
    }


    @Override
    public String remoteSearchForTermsOrdered(String[] terms) throws RemoteException {
        // Logic for searching terms with ordering
        System.out.println("Ordered search for terms: " + String.join(", ", terms));
        // Placeholder for actual ordered search result
        return "Ordered results for search terms.";
    }

    @Override
    public String remoteGetLinksToPointingTo(String url) throws RemoteException {
        // Logic to get links pointing to a specific URL
        System.out.println("Getting links pointing to: " + url);
        // Placeholder for actual links
        return "Links pointing to the URL.";
    }

    @Override
    public String remoteGetRealTimeSystemStatus() throws RemoteException {
        // Logic to get real-time system status
        System.out.println("Real-time system status requested.");
        // Placeholder for system status
        return "System status: OK";
    }

    @Override
    public String remoteCheckInvertedIndex() throws RemoteException{
        System.out.println("Checking inverted index! (2)");
        return "print_index";
    }


    @Override
    public synchronized void addUrlToQueue(String url) throws RemoteException {
        if (!urlQueue.contains(url)) {
            boolean isAdded = urlQueue.offer(url);
            if (isAdded) {
                notifyDownloader(); // Notify downloader threads
                System.out.println("URL added to queue: " + url);
            } else {
                // Debug print in case of failure
                System.err.println("Failed to add URL to queue: " + url);
            }
        } else {
            // Debug print if the URL is already in the queue
            System.out.println("URL already in queue, skipping: " + url);
        }
    }

   


    public void printQueueContent() {
        System.out.println("Current URLs in the Queue:");
        for (String url : urlQueue) {
            System.out.println(url);
        }
    }


    public synchronized void waitForUrl() throws InterruptedException{
        while (urlQueue.isEmpty()) {
            wait();
        }
    }
    
    public synchronized void notifyDownloader() {
        notifyAll();
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

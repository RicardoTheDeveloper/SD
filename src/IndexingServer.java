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
        // You would place the logic to handle the submitted URL here.
        // For example, you might add it to a queue to be processed by your web crawler.

        // Ensure that the URL is valid and can be processed
        if (isValidUrl(url)) {
            // Add the URL to a queue for the web crawler to process
            // Assuming you have a method in the Downloader class to add URLs to its queue
            // Inside your submitURL method
            Downloader downloader = new Downloader(); // Create an instance of Downloader
            downloader.enqueueURL(url); // Call the instance method on the object

            System.out.println("URL has been submitted and will be indexed: " + url);
        } else {
            System.out.println("Invalid URL submitted: " + url);
        }
    }

    private boolean isValidUrl(String url) {
        // Implement URL validation logic here
        // This might check for proper syntax, or if the URL is reachable, etc.
        return true; // Placeholder return value
    }
}

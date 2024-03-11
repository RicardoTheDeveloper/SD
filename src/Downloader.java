package src;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.io.IOException;

public class Downloader {
    
    public static void main(String[] args) {
        try {
            // Connecting to the RMI Registry
            Registry registry = LocateRegistry.getRegistry("localhost", 1099); // Replace "localhost" with the actual host if different
            GatewayInterface gateway = (GatewayInterface) registry.lookup("GatewayService");

            while (true) {
                // Fetch the next URL from the Gateway
                String url = gateway.fetchNextUrl();
                if (url != null) {
                    // Process the URL
                    try {
                        Document doc = Jsoup.connect(url).get();
                        // Perform your parsing here...
                        System.out.println("Processed URL: " + url);
                    } catch (IOException e) {
                        System.err.println("Error downloading content from: " + url);
                        e.printStackTrace();
                    }
                } else {
                    // If no URL was fetched, wait before trying again
                    Thread.sleep(5000); // Sleep for 5 seconds
                }
            }
        } catch (Exception e) {
            System.err.println("Downloader exception: " + e.toString());
            e.printStackTrace();
        }
    }
}

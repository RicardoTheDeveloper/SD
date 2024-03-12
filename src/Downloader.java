package src;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Downloader {

    private String multicastAddress = "224.3.2.1"; // Replace with your actual multicast address
    private int port = 4321; // Replace with your actual port

    

    // Method to send messages via multicast
    public void sendMulticastMessage(String message) {
        MulticastSocket socket = null;
        try {
            System.out.println("ENTREI NO SEND");
            socket = new MulticastSocket(); // Create socket without binding it (only for sending)
            InetAddress group = InetAddress.getByName(this.multicastAddress);
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, this.port);
            socket.send(packet);
            // No need for sleep here since you'll likely call this method per each URL processed
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    public static void main(String[] args) {
        try {
            // Connecting to the RMI Registry
            Registry registry = LocateRegistry.getRegistry("localhost", 1099); // Replace "localhost" with the actual host if different
            GatewayInterface gateway = (GatewayInterface) registry.lookup("GatewayService");
            Downloader downloader = new Downloader();



            while (true) {
                // Fetch the next URL from the Gateway
                String url = gateway.fetchNextUrl();
                if (url != null) {
                    // Process the URL
                    try {
                        Document doc = Jsoup.connect(url).get();
                        
                        // Print the title of the web page
                        System.out.println("Title: " + doc.title());


                        downloader.sendMulticastMessage(doc.title() + ";" + url); //enviar 
                        
                        // Print all paragraphs' text
                        Elements paragraphs = doc.select("p");
                        for (Element paragraph : paragraphs) {
                            System.out.println(paragraph.text());
                            downloader.sendMulticastMessage(paragraph.text() + ";" + url);
                        }

                        // Print all links with their text and href attributes
                        Elements links = doc.select("a[href]");
                        for (Element link : links) {
                            System.out.println("Text: " + link.text());
                            System.out.println("URL: " + link.attr("abs:href"));
                        }
                        
                        // ... Include additional processing if needed ...
                        
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

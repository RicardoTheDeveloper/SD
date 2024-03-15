package src;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Downloader implements Runnable {

    private static final AtomicInteger sharedCounter = new AtomicInteger(0);
    private final int threadId;
    private static int totalThreads = 5; // This should be set to the total number of downloader threads 

    private String multicastAddress = "224.3.2.1"; // multicast address
    private int port = 4321; // multicast port
    private GatewayInterface gateway;

    public Downloader(GatewayInterface gateway, int threadId){
        this.gateway = gateway;
        this.threadId = threadId;
    }


    

    

    // Método para enviar mensagens multicast (UDP!)
    public void sendMulticastMessage(String message) {
        MulticastSocket socket = null;
        try {
            socket = new MulticastSocket(); // Create Socket
            InetAddress group = InetAddress.getByName(this.multicastAddress);
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, this.port);
            socket.send(packet);
    
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    private String extractSnippet(String text) {
        // Let's take first 100 words for snippet for now
        String[] words = text.split("\\s+");
        StringBuilder snippetBuilder = new StringBuilder();
        for (int i = 0; i < words.length && i < 100; i++) {
            snippetBuilder.append(words[i]).append(" ");
        }
        return snippetBuilder.toString().trim();
    }

    


    public void handleIndexCommand(GatewayInterface gateway) {
        try {
            String url = gateway.fetchNextUrl();
            if (url != null && !url.isEmpty()) {
                Document doc = Jsoup.connect(url).get();
                String title = doc.title();
                String snippet = extractSnippet(doc.text()); // Your implementation to extract a snippet.
    
                // Send title and snippet once.
                sendMulticastMessage("type ; index ; title ; " + title + " ; snippet ; " + snippet);
    
                // For each word in the text, send a separate message.
                Elements paragraphs = doc.select("p");
                for (Element paragraph : paragraphs) {
                    for (String word : paragraph.text().split("\\s+")) {
                        if (!word.isEmpty()) {
                            sendMulticastMessage("type ; index ; word ; " + word.toLowerCase() + " ; url ; " + url);
                        }
                    }
                }
    
                // For each link, send a separate message.
                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    sendMulticastMessage("type ; link ; url ; " + link.attr("abs:href"));
                }
    
            }
        }
        
        catch (IOException e) {
            e.printStackTrace();
        }
         
    }
    
    private void handleSearchCommand(String searchTerms) {
        // Logic to perform the search
    }

    private void handleCheckingInvertedIndex(String command){

    }


    


    @Override
    public void run() {
        System.out.println("A THREAD WAS CREATED HERE");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                synchronized (sharedCounter) {
                    while (sharedCounter.get() % totalThreads != threadId) {
                        sharedCounter.wait();
                    }

                    // It's this thread's turn
                    gateway.waitForUrl();
                    handleIndexCommand(gateway);

                    // Update the counter and notify all waiting threads
                    sharedCounter.set((sharedCounter.get() + 1) % totalThreads);
                    sharedCounter.notifyAll();
                }
                
                // Optional: If no URL, sleep before the next check
                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
            System.err.println("Downloader interrupted: " + e.toString());
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Error in Downloader: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            // Connect to RMI registry
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            GatewayInterface gateway = (GatewayInterface) registry.lookup("GatewayService");

            // Start the downloader thread(s)
            int numberOfDownloaders = 5; // Or any number you choose
            for (int i = 0; i < numberOfDownloaders; i++) {
                Downloader downloader = new Downloader(gateway,i);
                Thread thread = new Thread(downloader);
                thread.start();
            }
        } catch (Exception e) {
            System.err.println("Downloader exception: " + e.toString());
            e.printStackTrace();
        }
    }
}

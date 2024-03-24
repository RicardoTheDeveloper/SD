package src;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.text.Normalizer;

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
    public void sendMulticastMessage(PageInfo pageInfo) {
        MulticastSocket socket = null;
        try {
            socket = new MulticastSocket(); // Create Socket
            InetAddress group = InetAddress.getByName(this.multicastAddress);

            // Compress and serialize PageInfo object to byte array
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteStream);
                 ObjectOutputStream objectOutputStream = new ObjectOutputStream(gzipOutputStream)) {
                objectOutputStream.writeObject(pageInfo);
            }
            byte[] compressedData = byteStream.toByteArray();

            // Check if the size of the compressed data is within the UDP packet size limit
            if (compressedData.length > 65507) {
                System.err.println("Compressed data is too large for UDP packet (" + compressedData.length + " bytes)");
                return; // Skipping sending this packet
            }

            DatagramPacket packet = new DatagramPacket(compressedData, compressedData.length, group, this.port);
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
    

    private String extractSnippet(Document doc) {
        Element firstParagraph = doc.selectFirst("p");
        return firstParagraph != null ? firstParagraph.text() : "";
    }

    


    public void handleIndexCommand(GatewayInterface gateway) {
        try {
            String currentUrl = gateway.fetchNextUrl();
            if (currentUrl != null && !currentUrl.isEmpty()) {
                Document doc = Jsoup.connect(currentUrl).get();
                String title = doc.title();
                String snippet = extractSnippet(doc);
    
                Set<String> uniqueWords = new HashSet<>();
                Map<String, Integer> wordCount = new HashMap<>();
    
                doc.body().select("p").forEach(p -> {
                    for (String word : p.text().split("\\s+")) {
                        String cleanedWord = word.replaceAll("[.,;:?!\"]+", "").toLowerCase();
                        if (!cleanedWord.isEmpty()) {
                            uniqueWords.add(cleanedWord); // Add original word
                            String normalizedWord = normalizeString(cleanedWord);
                            if (!normalizedWord.equals(cleanedWord)) {
                                uniqueWords.add(normalizedWord); // Add normalized word, if different
                            }
                        }
                    }
                });
    
                // Extract and send URLs found in the document
                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    String foundUrl = link.absUrl("href");
                    if (!foundUrl.isEmpty() && !foundUrl.equals(currentUrl)) {
                        //gateway.addUrlToQueue(foundUrl);
                    }
                }
    
                PageInfo pageInfo = new PageInfo(currentUrl, title, snippet, uniqueWords, wordCount);
                sendMulticastMessage(pageInfo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private String normalizeString(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("");
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
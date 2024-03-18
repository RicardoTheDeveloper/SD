package src;

import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IndexStorageBarrel extends UnicastRemoteObject implements IndexStorageBarrelInterface, Runnable {
    private MulticastSocket socket;
    private InetAddress group;
    private int port;

    private ConcurrentHashMap<String, PageInfo> pageInfoMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Set<String>> invertedIndex = new ConcurrentHashMap<>();
    private ExecutorService executorService;

    public IndexStorageBarrel(String multicastAddress, int port) throws IOException {
        this.port = port;
        this.group = InetAddress.getByName(multicastAddress);
        this.socket = new MulticastSocket(this.port);
        this.socket.joinGroup(this.group);
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] buffer = new byte[4096]; // Increased buffer size

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Process the received packet in a separate thread
                executorService.submit(() -> processPacket(packet));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        executorService.shutdown();
    }

    @Override
public String searchPagesWithTerms(String[] terms) throws RemoteException {
    System.out.println("Entered searchPagesWithTerms");
    Set<String> resultPages = new HashSet<>();
    boolean isFirstTerm = true;
    StringBuilder resultBuilder = new StringBuilder();

    for (String term : terms) {
        term = term.toLowerCase();
        Set<String> pagesWithTerm = new HashSet<>();

        // Search in content
        Set<String> contentPages = invertedIndex.get(term);
        if (contentPages != null) {
            pagesWithTerm.addAll(contentPages);
        }

        // Search in titles
        for (PageInfo pageInfo : pageInfoMap.values()) {
            if (pageInfo.getTitle().toLowerCase().contains(term)) {
                pagesWithTerm.add(pageInfo.getUrl());
            }
        }

        // Combine results
        if (isFirstTerm) {
            resultPages.addAll(pagesWithTerm);
            isFirstTerm = false;
        } else {
            resultPages.retainAll(pagesWithTerm);
        }
    }

    // Handling no results case
    if (resultPages.isEmpty() && !isFirstTerm) {
        for (String term : terms) {
            term = term.toLowerCase();
            Set<String> contentPages = invertedIndex.get(term);
            if (contentPages != null) {
                resultPages.addAll(contentPages);
            }
            for (PageInfo pageInfo : pageInfoMap.values()) {
                if (pageInfo.getTitle().toLowerCase().contains(term)) {
                    resultPages.add(pageInfo.getUrl());
                }
            }
        }
    }

    // Formatting the results
    int count = 0;
    for (String url : resultPages) {
        PageInfo pageInfo = fetchPageInfo(url);

        resultBuilder.append("Title: ").append(pageInfo.getTitle())
                     .append("\nURL: ").append(pageInfo.getUrl())
                     .append("\nSnippet: ").append(pageInfo.getSnippet())
                     .append("\n---------------------------------------\n");

        if (++count % 10 == 0 && count < resultPages.size()) {
            resultBuilder.append("\n-- Next group of results --\n\n");
        }
    }

    return count == 0 ? "No results found for the given terms." : resultBuilder.toString();
}


    public PageInfo fetchPageInfo(String url) {
        return pageInfoMap.get(url);
    }

    private void processPacket(DatagramPacket packet) {
        try {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(packet.getData());
            ObjectInputStream ois = new ObjectInputStream(byteStream);
            PageInfo pageInfo = (PageInfo) ois.readObject();

            pageInfoMap.put(pageInfo.getUrl(), pageInfo);
            updateInvertedIndex(pageInfo);
            printPageInfo(pageInfo);
            
            printInvertedIndex();

            savePageInfoToFile(pageInfo); // Save to file
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    private void updateInvertedIndex(PageInfo pageInfo) {
        String url = pageInfo.getUrl();
        pageInfo.getUniqueWords().forEach(word -> {
            invertedIndex.computeIfAbsent(word, k -> new HashSet<>()).add(url);
        });
    }

    private void printPageInfo(PageInfo pageInfo) {
        System.out.println("Received PageInfo:");
        System.out.println("URL: " + pageInfo.getUrl());
        System.out.println("Title: " + pageInfo.getTitle());
        System.out.println("Snippet: " + pageInfo.getSnippet());
        System.out.println("Words: " + pageInfo.getUniqueWords());
        pageInfo.showWordCount();
    }

    private void printInvertedIndex() {
        System.out.println("PRINTING INVERTED INDEX");
        invertedIndex.forEach((word, urls) -> {
            System.out.println("Word: " + word);
            urls.forEach(url -> System.out.println(" - " + url));
        });
    }


    private void savePageInfoToFile(PageInfo pageInfo) {
        try (FileOutputStream fileOut = new FileOutputStream("pageInfoObjects.dat", true); // Append mode
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(pageInfo);
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    public void searchAndDisplayResults(String[] searchTerms) {
        Set<String> pagesContainingAllTerms = new HashSet<>();
        boolean isFirstTerm = true;
    
        for (String term : searchTerms) {
            Set<String> pagesWithTerm = invertedIndex.get(term.toLowerCase());
            if (pagesWithTerm == null) {
                pagesContainingAllTerms.clear();
                break;
            }
            if (isFirstTerm) {
                pagesContainingAllTerms.addAll(pagesWithTerm);
                isFirstTerm = false;
            } else {
                pagesContainingAllTerms.retainAll(pagesWithTerm);
            }
        }
    
        displayResults(pagesContainingAllTerms);
    }
    
    private void displayResults(Set<String> pageUrls) {
        int count = 0;
        for (String url : pageUrls) {
            PageInfo pageInfo = fetchPageInfo(url);
            System.out.println("Title: " + pageInfo.getTitle());
            System.out.println("URL: " + pageInfo.getUrl());
            System.out.println("Snippet: " + pageInfo.getSnippet());
            System.out.println("---------------------------------------");
            
            if (++count == 10) break; // Show only the first 10 results
        }
    }
    

    public static void main(String[] args) {
        try {
            // Create the IndexStorageBarrel instance
            IndexStorageBarrel barrel = new IndexStorageBarrel("224.3.2.1", 4321);
    
            // The object is already exported in its constructor, so no need to export again
    
            // Register the object with the RMI registry
            Registry registry = LocateRegistry.createRegistry(1100);
            registry.rebind("IndexStorageBarrelService", barrel); // Directly pass the barrel object
    
            // Start the thread for processing incoming multicast packets
            new Thread(barrel).start();
    
            System.out.println("Index Storage Barrel RMI Service is ready.");
        } catch (IOException e) {
            System.out.println("Error starting Index Storage Barrel: " + e.getMessage());
            e.printStackTrace();
        }
    }
    


}

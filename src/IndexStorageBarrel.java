package src;

import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.text.Normalizer;
import java.util.regex.Pattern;

public class IndexStorageBarrel extends UnicastRemoteObject implements IndexStorageBarrelInterface, Runnable {
    private MulticastSocket socket;
    private InetAddress group;
    private int port;
    final int bufferSize = 10 * 1024 * 1024;

    private ConcurrentHashMap<String, PageInfo> pageInfoMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Set<String>> invertedIndex = new ConcurrentHashMap<>();
    private ExecutorService executorService;
    private ConcurrentHashMap<String, Set<String>> inboundLinks = new ConcurrentHashMap<>();

    public IndexStorageBarrel(String multicastAddress, int port) throws IOException {
        this.port = port;
        this.group = InetAddress.getByName(multicastAddress);
        this.socket = new MulticastSocket(this.port);
        this.socket.joinGroup(this.group);
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    private String normalizeString(String input) {
    String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
    Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    return pattern.matcher(normalized).replaceAll("").toLowerCase();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] buffer = new byte[bufferSize]; // Increased buffer size

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
        String normalizedTerm = normalizeString(term);
        Set<String> pagesWithTerm = new HashSet<>();

        // Search in content
        Set<String> contentPages = invertedIndex.get(normalizedTerm);
        if (contentPages != null) {
            pagesWithTerm.addAll(contentPages);
        }

        // Search in titles
        System.out.println("Searching for term in titles: " + term);
        for (PageInfo pageInfo : pageInfoMap.values()) {
            if (normalizeString(pageInfo.getTitle()).contains(normalizedTerm)) {
                System.out.println("Found in title: " + pageInfo.getUrl());
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


    private String formatSearchResults(Set<String> resultPages, IntPredicate newGroupCondition) {
        if (resultPages.isEmpty()) {
            return "No results found for the given terms.";
        }

        int count = 0;
        StringBuilder resultBuilder = new StringBuilder();
        for (String url : resultPages) {
            PageInfo pageInfo = fetchPageInfo(url);
            if (pageInfo != null) {
                resultBuilder.append("Title: ").append(pageInfo.getTitle())
                            .append("\nURL: ").append(pageInfo.getUrl())
                            .append("\nSnippet: ").append(pageInfo.getSnippet())
                            .append("\n---------------------------------------\n");

                if (newGroupCondition.test(count)) {
                    resultBuilder.append("\n-- Next group of results --\n\n");
                }
                count++;
            }
        }
        return resultBuilder.toString();
    }



    public PageInfo fetchPageInfo(String url) {
        return pageInfoMap.get(url);
    }

    private void processPacket(DatagramPacket packet) {
        try {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
            GZIPInputStream gzipInputStream = new GZIPInputStream(byteStream);
            ObjectInputStream ois = new ObjectInputStream(gzipInputStream);
            
            PageInfo pageInfo = (PageInfo) ois.readObject();
            
            // Iterate through existing PageInfo objects to find inbound links
            for (PageInfo existingPageInfo : pageInfoMap.values()) {
                if (existingPageInfo.getOutboundLinks().contains(pageInfo.getUrl())) {
                    // If the existing PageInfo links to the current one, add it to the inbound links
                    pageInfo.addInboundLink(existingPageInfo.getUrl());
                }
            }
    
            // Update pageInfoMap with the new PageInfo
            pageInfoMap.put(pageInfo.getUrl(), pageInfo);
    
            // Update inverted index
            updateInvertedIndex(pageInfo);
    
            // Print for debugging
            System.out.println("Processed PageInfo: " + pageInfo.getUrl());
            System.out.println("Inbound Links: " + pageInfo.getInboundLinks());
            
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
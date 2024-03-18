package src;

import java.net.*;
import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IndexStorageBarrel implements Runnable {
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
                byte[] buffer = new byte[1024];
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
            IndexStorageBarrel barrel = new IndexStorageBarrel("224.3.2.1", 4321);
            new Thread(barrel).start();
        } catch (IOException e) {
            System.out.println("Error starting Index Storage Barrel: " + e.getMessage());
        }
    }
}

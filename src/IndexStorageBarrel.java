package src;

import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;
import java.text.Normalizer;
import java.util.regex.Pattern;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class IndexStorageBarrel extends UnicastRemoteObject implements IndexStorageBarrelInterface, Runnable {
    private String id; // New field to store the ID
    private MulticastSocket socket;
    private InetAddress group;
    private int port;
    final int bufferSize = 10 * 1024 * 1024;

    private ConcurrentHashMap<String, PageInfo> pageInfoMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Set<String>> invertedIndex = new ConcurrentHashMap<>();
    private ExecutorService executorService;
    private ConcurrentHashMap<String, Set<String>> inboundLinks = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Integer> searchFrequencies = new ConcurrentHashMap<>();
    private List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
    
    private Map<String, ByteArrayOutputStream> messageBuffers = new ConcurrentHashMap<>();
    

    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Boolean>> chunksReceived = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, byte[]>> messageDataBuffers = new ConcurrentHashMap<>();

  
    private Map<String, Future<?>> nackTasks = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    


    
    

    private static final String DATA_FILE = "IndexStorageBarrelData_";

    public IndexStorageBarrel(String id,String multicastAddress, int port) throws IOException {
        this.id = id; // Store the provided ID
        this.port = port;
        this.group = InetAddress.getByName(multicastAddress);
        this.socket = new MulticastSocket(this.port);
        this.socket.joinGroup(this.group);
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        loadState(); // Load saved state from file
        synchronizeData(); // Synchronize data with other barrels
    }

    private void loadState() {
        File file = new File(DATA_FILE + id + ".dat");
        if (file.exists() && file.length() > 0) { // Check that the file exists and is not empty
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                pageInfoMap = (ConcurrentHashMap<String, PageInfo>) in.readObject();
                invertedIndex = (ConcurrentHashMap<String, Set<String>>) in.readObject();
                
                System.out.println("PageInfoMap: ");
                pageInfoMap.forEach((url, pageInfo) -> System.out.println("URL: " + url + ", PageInfo: " + pageInfo));
                System.out.println("Inverted Index: ");
                invertedIndex.forEach((word, urls) -> {
                    System.out.println("Word: " + word + ", URLs: " + urls);
    });
                // ...
            } catch (EOFException e) {
                System.err.println("Reached end of file unexpectedly. Possibly corrupted file.");
                e.printStackTrace();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }


    private synchronized void saveState() {
        File file = new File(DATA_FILE + id + ".dat");
        RandomAccessFile raf = null;
        FileChannel fileChannel = null;
        FileLock lock = null;
        try {
            raf = new RandomAccessFile(file, "rw");
            fileChannel = raf.getChannel();
    
            System.out.println("Attempting to acquire file lock...");
            lock = fileChannel.lock(); // Lock the file
            System.out.println("File lock acquired. Truncating file...");
            fileChannel.truncate(0); // Clear the file content
    
            System.out.println("Writing objects to file...");
            ObjectOutputStream out = new ObjectOutputStream(Channels.newOutputStream(fileChannel));
            out.writeObject(pageInfoMap);
            out.writeObject(invertedIndex);
            // Write other necessary objects
            out.close();
            System.out.println("Objects written to file successfully.");
        } catch (IOException e) {
            System.err.println("IO Exception in saveState: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Release the lock and close resources in the finally block
            try {
                if (lock != null && lock.isValid()) {
                    lock.release();
                    System.out.println("File lock released.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (fileChannel != null) {
                    fileChannel.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    


    

    private void printLoadedData() {
        System.out.println("Loaded Data for Barrel ID: " + id);
        System.out.println("PageInfoMap: ");
        pageInfoMap.forEach((url, pageInfo) -> System.out.println("URL: " + url + ", PageInfo: " + pageInfo));
        System.out.println("Inverted Index: ");
        invertedIndex.forEach((word, urls) -> {
            System.out.println("Word: " + word + ", URLs: " + urls);
        });
    }



    private void synchronizeData() {
        for (int port = 1100; port <= 1200; port++) {
            try {
                Registry registry = LocateRegistry.getRegistry("localhost", port);
                String[] boundNames = registry.list();
                for (String name : boundNames) {
                    if (name.startsWith("IndexStorageBarrelService_")) {
                        String otherId = name.substring(name.lastIndexOf('_') + 1);
                        if (!otherId.equals(this.id)) { // Ensure not to synchronize with self
                            try {
                                IndexStorageBarrelInterface otherBarrel = (IndexStorageBarrelInterface) registry.lookup(name);
                                this.pageInfoMap = otherBarrel.getPageInfoMap();
                                this.invertedIndex = otherBarrel.getInvertedIndex();
                                this.saveState(); // Save the copied state
                                System.out.println("Synchronized data from barrel ID: " + otherId);

                                printLoadedData();
                                return; // Data synchronized successfully, exit the loop
                            } catch (Exception e) {
                                System.err.println("Failed to synchronize data from barrel ID: " + otherId);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error during synchronization process on port: " + port);
            }
        }
        System.err.println("Failed to synchronize data from any barrel.");
    }
    


    
    

    private String normalizeString(String input) {
    String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
    Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    return pattern.matcher(normalized).replaceAll("").toLowerCase();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() && !executorService.isShutdown()) {
            try {
                byte[] buffer = new byte[bufferSize];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Check if executorService has been shutdown
                if (executorService.isShutdown()) {
                    break; // Exit the loop if the executor service is shut down
                }

                // Process the received packet in a separate thread
                executorService.submit(() -> processPacket(packet));
            } catch (IOException e) {
                if (!executorService.isShutdown()) {
                    e.printStackTrace();
                }
            }
        }
    }




    @Override
public String searchPagesWithTerms(String[] terms) throws RemoteException {
    


    System.out.println("Entered searchPagesWithTerms");
    Map<PageInfo, Integer> pageInfoInboundLinkCountMap = new HashMap<>();
    
    Set<String> pagesWithAllTerms = new HashSet<>();
    boolean isFirstTerm = true;
    
    long startTime = System.nanoTime();
    // Find pages that contain all terms
    for (String term : terms) {
        String normalizedTerm = normalizeString(term);
        Set<String> pagesWithTerm = invertedIndex.get(normalizedTerm);
        if (pagesWithTerm == null || pagesWithTerm.isEmpty()) {
            return "No results found for the given terms.";
        }
        if (isFirstTerm) {
            pagesWithAllTerms.addAll(pagesWithTerm);
            isFirstTerm = false;
        } else {
            pagesWithAllTerms.retainAll(pagesWithTerm); // Intersection of sets
        }
    }
    
    // Count inbound links for these pages
    for (String url : pagesWithAllTerms) {
        PageInfo pageInfo = pageInfoMap.get(url);
        int inboundLinksCount = inboundLinks.getOrDefault(url, Collections.emptySet()).size();
        pageInfoInboundLinkCountMap.put(pageInfo, inboundLinksCount);
    }
    
   // Prepare a list of PageInfo from the set of URLs
    List<PageInfo> pageInfoList = pagesWithAllTerms.stream()
        .map(url -> pageInfoMap.get(url))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

    // Sort PageInfo objects based on the number of inbound links in descending order
    pageInfoList.sort((page1, page2) -> Integer.compare(page2.getInboundLinks().size(), page1.getInboundLinks().size()));


    long searchTime = System.nanoTime() - startTime;
    responseTimes.add(searchTime); // Add the response time to the list

    // Format the search results
    StringBuilder resultBuilder = new StringBuilder();
    final int resultsPerGroup = 10;
    int groupNumber = 1;
    int currentGroupIndex = 0;

    for (PageInfo pageInfo : pageInfoList) {
        if (currentGroupIndex % resultsPerGroup == 0) {
            if (currentGroupIndex > 0) {
                resultBuilder.append("\n-- End of Group ").append(groupNumber - 1).append(" --\n\n");
            }
            resultBuilder.append("-- Group ").append(groupNumber).append(" --\n");
            groupNumber++;
        }
        resultBuilder.append("Title: ").append(pageInfo.getTitle())
                     .append("\nURL: ").append(pageInfo.getUrl())
                     .append("\nSnippet: ").append(pageInfo.getSnippet())
                     .append("\nInbound Links: ").append(pageInfo.getInboundLinks().size())
                     .append("\n---------------------------------------\n");
        currentGroupIndex++;
    }

    if (currentGroupIndex % resultsPerGroup != 0) { // Close the last group if it contains results
        resultBuilder.append("\n-- End of Group ").append(groupNumber - 1).append(" --\n");
    }

    
    
    // Record each search term frequency
    for (String term : terms) {
        searchFrequencies.merge(term, 1, Integer::sum);
    }

    saveState();

    return resultBuilder.toString();
}


@Override
public String getInboundLinks(String url) throws RemoteException {
    PageInfo pageInfo = pageInfoMap.get(url);
    if (pageInfo == null) {
        return "No page found with URL: " + url;
    }
    Set<String> inboundLinks = pageInfo.getInboundLinks();
    if (inboundLinks.isEmpty()) {
        return "No inbound links found for URL: " + url;
    }
    StringBuilder linksResult = new StringBuilder("Inbound links for " + url + ":\n");
    for (String link : inboundLinks) {
        linksResult.append(link).append("\n");
    }
    return linksResult.toString();
}

@Override
public Map<String, Integer> getSearchFrequencies() throws RemoteException {
    return new HashMap<>(searchFrequencies); // Return a copy to avoid concurrent modification exceptions
}

@Override
public double getAverageResponseTime() throws RemoteException {
    synchronized (responseTimes) {
        return responseTimes.stream()
                .mapToDouble(Long::doubleValue)
                .average()
                .orElse(0.0) / 1e8; // Convert nanoseconds to tenths of a second
    }
}


    @Override
    public ConcurrentHashMap<String, PageInfo> getPageInfoMap() throws RemoteException {
        // Return a deep copy if necessary to prevent modifications
        return new ConcurrentHashMap<>(pageInfoMap);
    }

    @Override
    public ConcurrentHashMap<String, Set<String>> getInvertedIndex() throws RemoteException {
        // Return a deep copy if necessary to prevent modifications
        return new ConcurrentHashMap<>(invertedIndex);
    }




    



    public PageInfo fetchPageInfo(String url) {
        return pageInfoMap.get(url);
    }

    

   private void processPacket(DatagramPacket packet) {
    try {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
        try (ObjectInputStream ois = new ObjectInputStream(byteStream)) {
            Object obj = ois.readObject();
            if (obj instanceof MessageChunk) {
                MessageChunk chunk = (MessageChunk) obj;

                // Decompress chunk data
                byte[] decompressedData;
                try (ByteArrayInputStream bais = new ByteArrayInputStream(chunk.getChunkData());
                     GZIPInputStream gzipInputStream = new GZIPInputStream(bais)) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] tempBuffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = gzipInputStream.read(tempBuffer)) != -1) {
                        buffer.write(tempBuffer, 0, bytesRead);
                    }
                    decompressedData = buffer.toByteArray();
                }

                ConcurrentHashMap<Integer, Boolean> receivedStatus = chunksReceived.computeIfAbsent(chunk.getMessageId(), k -> new ConcurrentHashMap<>());
                ConcurrentHashMap<Integer, byte[]> dataBuffer = messageDataBuffers.computeIfAbsent(chunk.getMessageId(), k -> new ConcurrentHashMap<>());

                receivedStatus.put(chunk.getIndex(), true);
                dataBuffer.put(chunk.getIndex(), decompressedData);

                System.out.println("Chunk received: " + chunk.getIndex() + " of " + chunk.getTotalChunks() + " for message " + chunk.getMessageId());

                // Check if this is the first chunk and start the timer
                if (receivedStatus.size() == 1 && !nackTasks.containsKey(chunk.getMessageId())) {
                    ScheduledFuture<?> nackTask = scheduler.schedule(() -> {
                        if (receivedStatus.size() < chunk.getTotalChunks()) {
                            Set<Integer> missingChunks = new HashSet<>();
                            for (int i = 0; i < chunk.getTotalChunks(); i++) {
                                if (!receivedStatus.getOrDefault(i, false)) {
                                    missingChunks.add(i);
                                }
                            }
                            if (!missingChunks.isEmpty()) {
                                sendNackMessage(chunk.getMessageId(), missingChunks, chunk.getDownloaderNackPort());
                            }
                        }
                    }, 2, TimeUnit.SECONDS);
                    nackTasks.put(chunk.getMessageId(), nackTask);
                }

                synchronized (receivedStatus){
                    // Verify if all chunks are received
                if (receivedStatus.size() == chunk.getTotalChunks() && receivedStatus.values().stream().allMatch(Boolean.TRUE::equals)) {
                    ByteArrayOutputStream completeMessage = new ByteArrayOutputStream();

                    // Ensure chunks are in correct order before writing
                    for (int i = 0; i < chunk.getTotalChunks(); i++) {
                        byte[] chunkData = dataBuffer.get(i);
                        if (chunkData != null) {
                            completeMessage.write(chunkData);
                        } else {
                            System.out.println("Missing chunk data for index " + i);
                            return; // Exit if data is missing
                        }
                    }

                    // Deserialize data to obtain PageInfo object
                    byte[] completeData = completeMessage.toByteArray();
                    try (ObjectInputStream oisPageInfo = new ObjectInputStream(new ByteArrayInputStream(completeData))) {
                        PageInfo pageInfo = (PageInfo) oisPageInfo.readObject();
                        System.out.println("PageInfo reconstructed: " + pageInfo.getTitle());
                        
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
                        saveState();

                        //send termination message
                        sendTerminationMessage(chunk.getThreadID(),chunk.getDownloaderNackPort());
                        
                    }

                    // Clear trackers for this message
                    receivedStatus.clear();
                    dataBuffer.clear();
                    nackTasks.remove(chunk.getMessageId()); // Remove NACK task entry
                }

                }
                 
            }
        }
    } catch (IOException | ClassNotFoundException e) {
        e.printStackTrace();
    }
}
    



    private void sendNackMessage(String messageId, Set<Integer> missingChunks, int downloaderNackPort) {
    try {
        InetAddress address = InetAddress.getByName("localhost"); // Adjust as needed for your network setup

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(new NACKMessage(messageId, missingChunks));
        }
        byte[] nackData = baos.toByteArray();

        // Use a standard DatagramSocket instead of the multicast socket
        try (DatagramSocket socket = new DatagramSocket()) {
            DatagramPacket nackPacket = new DatagramPacket(nackData, nackData.length, address, downloaderNackPort);
            socket.send(nackPacket);
            System.out.println("NACK message sent to thread with NACK port: " + downloaderNackPort);
        }
    } catch (IOException e) {
        System.err.println("Error sending NACK message: " + e.getMessage());
        e.printStackTrace();
    }
}


    private void sendTerminationMessage(int threadId,int nackPort) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName("localhost"); // or the appropriate address
            TerminationMessage terminationMessage = new TerminationMessage(threadId);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(terminationMessage);
            byte[] data = baos.toByteArray();
    
            DatagramPacket packet = new DatagramPacket(data, data.length, address, nackPort);
            socket.send(packet);
            System.out.println("TERMINATION MESSAGE SENT TO THREAD WITH nackPort: "+nackPort);
        } catch (IOException e) {
            System.err.println("Error sending termination message: " + e.getMessage());
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
    
    public void cleanupResources() {
        // Close multicast socket if open
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        // Perform other resource cleanup if necessary
    }


    public void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Now executorService can be accessed directly as it's in instance context
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS))
                        System.err.println("ExecutorService did not terminate");
                }
            } catch (InterruptedException ie) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
    
            saveState();
            cleanupResources();
            System.out.println("Shutdown hook completed.");
        }));
    }

    public static void main(String[] args) {
        try {
            // Check if the correct number of arguments are provided
            if (args.length != 5) {
                System.out.println("Usage: java IndexStorageBarrel <id> <multicast-address> <multicast-port> <rmi-registry-port> <ip host>");
                return;
            }
    
            // Extract command line arguments
            String id = args[0];
            String multicastAddress = args[1];
            int port = Integer.parseInt(args[2]);
            int registryPort = Integer.parseInt(args[3]);
            String rmiBindAddress = args[4]; // Endereço IP para ligação RMI
    
            // Output the ID to confirm it's being used
            System.out.println("Starting Index Storage Barrel with ID: " + id);
    
            // Create the IndexStorageBarrel instance with the provided ID, multicast address, and port
            IndexStorageBarrel barrel = new IndexStorageBarrel(id, multicastAddress, port);

            barrel.addShutdownHook();
    
            // Register the object with the RMI registry using the ID in the service name
            System.setProperty("java.rmi.server.hostname", rmiBindAddress);
            Registry registry = LocateRegistry.createRegistry(registryPort);
            registry.rebind("IndexStorageBarrelService_" + id, barrel);
    
            // Start the thread for processing incoming multicast packets
            new Thread(barrel).start();


            
            


    
            System.out.println("Index Storage Barrel RMI Service with ID " + id + " is ready at " +
                               multicastAddress + ":" + port + " with RMI registry on port " + registryPort);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format for port or registry port.");
        } catch (IOException e) {
            System.out.println("Error starting Index Storage Barrel: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
}
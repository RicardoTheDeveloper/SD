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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.text.Normalizer;

/**
 * The `IndexStorageBarrel` class represents a storage barrel for indexing web pages.
 * It implements the `IndexStorageBarrelInterface` and `Runnable` interfaces.
 * 
 * This class is responsible for storing and synchronizing data related to web pages,
 * maintaining an inverted index, and performing search operations on the indexed data.
 * 
 * The `IndexStorageBarrel` class uses a multicast socket to receive packets containing
 * web page data, processes the packets in a separate thread, and updates the internal
 * data structures accordingly.
 * 
 * The class also provides methods for searching web pages based on given terms, and
 * retrieving the search results in a sorted manner based on the number of inbound links.
 * 
 * The state of the `IndexStorageBarrel` object can be saved to a file and loaded from
 * the file to maintain data persistence across multiple runs of the application.
 * 
 * This class is designed to be used in a distributed system where multiple instances of
 * `IndexStorageBarrel` can synchronize their data with each other to maintain a consistent
 * view of the indexed web pages.
 */
public class IndexStorageBarrel extends UnicastRemoteObject implements IndexStorageBarrelInterface, Runnable {

    private String id;
    private MulticastSocket socket;
    private InetAddress group;
    private int port;
    final int bufferSize = 10 * 1024 * 1024;
    private String localIpAddress;
    private String remoteIpAddress;
    private ConcurrentHashMap<String, PageInfo> pageInfoMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Set<String>> invertedIndex = new ConcurrentHashMap<>();
    private ExecutorService executorService;
    private ConcurrentHashMap<String, Set<String>> inboundLinks = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Integer> searchFrequencies = new ConcurrentHashMap<>();
    private List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, Boolean>> chunksReceived = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ConcurrentHashMap<Integer, byte[]>> messageDataBuffers = new ConcurrentHashMap<>();
    private Map<String, Future<?>> nackTasks = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final String DATA_FILE = "IndexStorageBarrelData_";

    /**
     * Constructs a new IndexStorageBarrel object with the specified parameters.
     *
     * @param id the ID of the IndexStorageBarrel
     * @param multicastAddress the multicast address to join
     * @param port the port number to use for communication
     * @param localIpAddress the local IP address of the IndexStorageBarrel
     * @param remoteIpAddress the remote IP address of the IndexStorageBarrel
     * @throws IOException if an I/O error occurs
     */
    public IndexStorageBarrel(String id,String multicastAddress, int port, String localIpAddress, String remoteIpAddress) throws IOException {

        this.id = id;
        this.port = port;
        this.localIpAddress = localIpAddress;
        this.remoteIpAddress = remoteIpAddress;
        this.group = InetAddress.getByName(multicastAddress);
        this.socket = new MulticastSocket(port);
        this.socket.joinGroup(group);
        
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        loadState();
        synchronizeData();
        
    }

    /**
     * Loads the state of the IndexStorageBarrel from a file.
     * If the file exists and is not empty, it reads the stored data and updates the pageInfoMap and invertedIndex accordingly.
     * The loaded data is printed to the console for debugging purposes.
     * If an EOFException occurs, it indicates that the file may be corrupted.
     * Any IOException or ClassNotFoundException that occurs during the loading process is also printed to the console.
     */
    private void loadState() {

        File file = new File(DATA_FILE + id + ".dat");

        if (file.exists() && file.length() > 0) {

            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                
                pageInfoMap = (ConcurrentHashMap<String, PageInfo>) in.readObject();
                invertedIndex = (ConcurrentHashMap<String, Set<String>>) in.readObject();
                
                System.out.println("PageInfoMap: ");
                pageInfoMap.forEach((url, pageInfo) -> System.out.println("URL: " + url + ", PageInfo: " + pageInfo));
                System.out.println("Inverted Index: ");
                invertedIndex.forEach((word, urls) -> {
                    System.out.println("Word: " + word + ", URLs: " + urls);
                });

            } 
            
            catch (EOFException e) {
                System.err.println("Reached end of file unexpectedly. Possibly corrupted file.");
                e.printStackTrace();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Saves the state of the IndexStorageBarrel object to a file.
     * The state includes the pageInfoMap and invertedIndex data structures.
     * This method is synchronized to ensure thread safety.
     */
    private synchronized void saveState() {

        File file = new File(DATA_FILE + id + ".dat");

        try (RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel fileChannel = raf.getChannel()) {
            FileLock lock = null;

            try {
                lock = fileChannel.tryLock();
                if (lock != null) {
                    fileChannel.truncate(0);
                    
                    try (ObjectOutputStream out = new ObjectOutputStream(Channels.newOutputStream(fileChannel))) {
                        out.writeObject(pageInfoMap);
                        out.writeObject(invertedIndex);
                    }

                } else {
                    System.err.println("Unable to acquire file lock, state not saved.");
                }
            } catch (IOException e) {
                System.err.println("IO Exception in saveState: " + e.getMessage());
            } finally {
                if (lock != null && lock.isValid()) {
                    lock.release();
                }
            }
        } catch (OverlappingFileLockException e) {
            System.err.println("Overlapping File Lock Exception: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IOException when attempting to lock file: " + e.getMessage());
        }

        if (Thread.interrupted()) {
            System.err.println("Thread interrupted during saveState, state may not have been saved.");
        }
    }

    /**
     * Synchronizes data with other instances of the IndexStorageBarrel service.
     * This method iterates through a list of IP addresses and ports, attempts to connect to each one,
     * and retrieves the pageInfoMap and invertedIndex from a remote IndexStorageBarrel instance.
     * If a successful connection is made, the local pageInfoMap and invertedIndex are updated with the retrieved data,
     * and the state is saved. The method stops as soon as a successful synchronization occurs.
     * If no successful synchronization occurs, an error message is printed.
     */
    private void synchronizeData() {
        String[] ipAddresses = {localIpAddress, remoteIpAddress};
    
        for (String ipAddress : ipAddresses) {
            for (int port = 1100; port <= 1105; port++) {
                try {
                    Registry registry = LocateRegistry.getRegistry(ipAddress, port);
                    String[] boundNames = registry.list();
                    for (String name : boundNames) {
                        if (name.startsWith("IndexStorageBarrelService_")) {
                            String otherId = name.substring(name.lastIndexOf('_') + 1);
                            if (!otherId.equals(this.id)) {
                                try {
                                    IndexStorageBarrelInterface otherBarrel = (IndexStorageBarrelInterface) registry.lookup(name);
                                    this.pageInfoMap = otherBarrel.getPageInfoMap();
                                    this.invertedIndex = otherBarrel.getInvertedIndex();
                                    saveState();
                                    System.out.println("Synchronized data from barrel ID: " + otherId + " at IP: " + ipAddress);
                                    return;
                                } catch (Exception e) {
                                    System.err.println("Failed to synchronize with barrel ID: " + otherId + " at IP: " + ipAddress + " on port: " + port);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Cannot reach registry at IP: " + ipAddress + " on port: " + port);
                }
            }
        }
        System.err.println("Failed to synchronize with any barrels.");
    }
    
    /**
     * Normalizes a given string by removing diacritical marks and converting it to lowercase.
     *
     * @param input the string to be normalized
     * @return the normalized string
     */
    private String normalizeString(String input) {
    String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
    Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    return pattern.matcher(normalized).replaceAll("").toLowerCase();
    }

    /**
     * This method represents the main logic of the IndexStorageBarrel thread.
     * It continuously receives DatagramPackets from the socket and processes them.
     * The method runs until the thread is interrupted or the executor service is shutdown.
     */
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted() && !executorService.isShutdown()) {

            try {
                byte[] buffer = new byte[bufferSize];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                if (executorService.isShutdown()) {
                    break;
                }

                executorService.submit(() -> processPacket(packet));
            } catch (IOException e) {
                if (!executorService.isShutdown()) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * Searches for pages that contain all the given terms and returns the results as a formatted string.
     * 
     * @param terms an array of terms to search for
     * @return a string containing the search results
     * @throws RemoteException if a remote exception occurs during the search
     */
    @Override
    public List<PageInfo> searchPagesWithTerms(String[] terms) throws RemoteException {
        
        System.out.println("Entered searchPagesWithTerms");
        Map<PageInfo, Integer> pageInfoInboundLinkCountMap = new HashMap<>();
        Set<String> pagesWithAllTerms = new HashSet<>();
        boolean isFirstTerm = true;
        long startTime = System.nanoTime();

        for (String term : terms) {
            String normalizedTerm = normalizeString(term);
            Set<String> pagesWithTerm = invertedIndex.get(normalizedTerm);
            if (pagesWithTerm == null || pagesWithTerm.isEmpty()) {
                return null;
            }
            if (isFirstTerm) {
                pagesWithAllTerms.addAll(pagesWithTerm);
                isFirstTerm = false;
            } else {
                pagesWithAllTerms.retainAll(pagesWithTerm);
            }
        }
        
        for (String url : pagesWithAllTerms) {
            PageInfo pageInfo = pageInfoMap.get(url);
            int inboundLinksCount = inboundLinks.getOrDefault(url, Collections.emptySet()).size();
            pageInfoInboundLinkCountMap.put(pageInfo, inboundLinksCount);
        }
        
        List<PageInfo> pageInfoList = pagesWithAllTerms.stream()
            .map(url -> pageInfoMap.get(url))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        pageInfoList.sort((page1, page2) -> Integer.compare(page2.getInboundLinks().size(), page1.getInboundLinks().size()));

        long searchTime = System.nanoTime() - startTime;
        responseTimes.add(searchTime);

        

        for (String term : terms) {
            searchFrequencies.merge(term, 1, Integer::sum);
        }

        saveState();

        return pageInfoList;
    }


    /**
     * Returns the inbound links for a given URL.
     * 
     * @param url the URL for which to retrieve the inbound links
     * @return a string containing the inbound links for the specified URL
     * @throws RemoteException if a remote exception occurs
     */
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

    /**
        * Returns a map containing the search frequencies.
        *
        * @return a map containing the search frequencies
        * @throws RemoteException if a remote exception occurs
        */
    @Override
    public Map<String, Integer> getSearchFrequencies() throws RemoteException {
        return new HashMap<>(searchFrequencies);
    }

    /**
     * Returns the average response time of the IndexStorageBarrel.
     *
     * @return the average response time in seconds
     * @throws RemoteException if a remote exception occurs
     */
    @Override
    public double getAverageResponseTime() throws RemoteException {
        synchronized (responseTimes) {
            return responseTimes.stream()
                    .mapToDouble(Long::doubleValue)
                    .average()
                    .orElse(0.0) / 1e8;
        }
    }

    /**
     * Returns a ConcurrentHashMap containing the page information map.
     *
     * @return a ConcurrentHashMap containing the page information map.
     * @throws RemoteException if a remote exception occurs.
     */
    @Override
    public ConcurrentHashMap<String, PageInfo> getPageInfoMap() throws RemoteException {
        return new ConcurrentHashMap<>(pageInfoMap);
    }

    /**
     * Returns the inverted index.
     *
     * @return the inverted index as a ConcurrentHashMap, where the keys are terms and the values are sets of documents.
     * @throws RemoteException if a remote exception occurs.
     */
    @Override
    public ConcurrentHashMap<String, Set<String>> getInvertedIndex() throws RemoteException {
        return new ConcurrentHashMap<>(invertedIndex);
    }

    /**
     * Retrieves the PageInfo object associated with the given URL.
     *
     * @param url the URL for which to fetch the PageInfo
     * @return the PageInfo object associated with the given URL, or null if not found
     */
    public PageInfo fetchPageInfo(String url) {
        return pageInfoMap.get(url);
    }

    /**
     * Processes a DatagramPacket received by the IndexStorageBarrel.
     * This method reads the data from the packet, decompresses it, and stores it in the appropriate data structures.
     * If all the chunks of a message have been received, it reconstructs the complete message, updates the inverted index, and saves the state.
     * Finally, it sends a termination message to the downloader.
     *
     * @param packet The DatagramPacket to be processed.
     */
   private void processPacket(DatagramPacket packet) {

        try {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
            
            try (ObjectInputStream ois = new ObjectInputStream(byteStream)) {
                Object obj = ois.readObject();
                if (obj instanceof MessageChunk) {
                    MessageChunk chunk = (MessageChunk) obj;

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
                    
                    if (receivedStatus.size() == chunk.getTotalChunks() && receivedStatus.values().stream().allMatch(Boolean.TRUE::equals)) {
                        ByteArrayOutputStream completeMessage = new ByteArrayOutputStream();

                        for (int i = 0; i < chunk.getTotalChunks(); i++) {
                            byte[] chunkData = dataBuffer.get(i);
                            if (chunkData != null) {
                                completeMessage.write(chunkData);
                            } else {
                                System.out.println("Missing chunk data for index " + i);
                                return;
                            }
                        }

                        byte[] completeData = completeMessage.toByteArray();
                        try (ObjectInputStream oisPageInfo = new ObjectInputStream(new ByteArrayInputStream(completeData))) {
                            PageInfo pageInfo = (PageInfo) oisPageInfo.readObject();
                            System.out.println("PageInfo reconstructed: " + pageInfo.getTitle());
                            
                            for (PageInfo existingPageInfo : pageInfoMap.values()) {
                                if (existingPageInfo.getOutboundLinks().contains(pageInfo.getUrl())) {
                                    pageInfo.addInboundLink(existingPageInfo.getUrl());
                                }
                            }
                    
                            pageInfoMap.put(pageInfo.getUrl(), pageInfo);
                    
                            updateInvertedIndex(pageInfo);
                            saveState();

                            sendTerminationMessage(chunk.getThreadID(),chunk.getDownloaderNackPort());
                            
                        }

                        receivedStatus.clear();
                        dataBuffer.clear();
                        nackTasks.remove(chunk.getMessageId());
                    }

                    }
                    
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Sends a NACK (Negative Acknowledgement) message to a downloader thread.
     * The NACK message contains the message ID and a set of missing chunks.
     *
     * @param messageId         the ID of the message
     * @param missingChunks     the set of missing chunks
     * @param downloaderNackPort   the port number of the downloader thread to send the NACK message to
     */
    private void sendNackMessage(String messageId, Set<Integer> missingChunks, int downloaderNackPort) {
        try {
            InetAddress address = InetAddress.getByName("localhost");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(new NACKMessage(messageId, missingChunks));
            }
            byte[] nackData = baos.toByteArray();

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

    /**
     * Sends a termination message to a specified thread with the given nackPort.
     * 
     * @param threadId the ID of the thread to send the termination message to
     * @param nackPort the port number to send the termination message to
     */
    private void sendTerminationMessage(int threadId,int nackPort) {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress address = InetAddress.getByName("localhost");
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
    
    /**
     * Updates the inverted index with the given page information.
     *
     * @param pageInfo The PageInfo object containing the URL and unique words of a page.
     */
    private void updateInvertedIndex(PageInfo pageInfo) {
        String url = pageInfo.getUrl();
        pageInfo.getUniqueWords().forEach(word -> {
            invertedIndex.computeIfAbsent(word, k -> new HashSet<>()).add(url);
        });
    }

    /**
     * Searches for pages that contain all the given search terms and displays the results.
     *
     * @param searchTerms an array of search terms to be used for searching
     */
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
    
    /**
     * Displays the results of a set of page URLs.
     * 
     * @param pageUrls the set of page URLs to display results for
     */
    private void displayResults(Set<String> pageUrls) {
        int count = 0;
        for (String url : pageUrls) {
            PageInfo pageInfo = fetchPageInfo(url);
            System.out.println("Title: " + pageInfo.getTitle());
            System.out.println("URL: " + pageInfo.getUrl());
            System.out.println("Snippet: " + pageInfo.getSnippet());
            System.out.println("---------------------------------------");
            
            if (++count == 10) break;
        }
    }
    
    /**
     * Cleans up any resources used by the IndexStorageBarrel.
     * This method closes the socket if it is open.
     */
    public void cleanupResources() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    /**
     * Adds a shutdown hook to gracefully shut down the IndexStorageBarrel.
     * 
     * @param gatewayStub the GatewayInterface stub used to notify the Gateway about the barrel becoming inactive
     */
    public void addShutdownHook(GatewayInterface gatewayStub) {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            try {
                if (gatewayStub != null) {
                    gatewayStub.notifyBarrelInactive(this.id);
                }
            } catch (RemoteException e) {
                System.err.println("Error notifying the Gateway: " + e.getMessage());
                e.printStackTrace();
            }
    
            executorService.shutdown();

            try {

                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {

                    System.out.println("ExecutorService still has tasks running after 60 seconds. Attempting to shut down now.");
                    List<Runnable> droppedTasks = executorService.shutdownNow();
                    System.out.println("ExecutorService did not terminate. Dropped tasks: " + droppedTasks);
                    if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                        System.err.println("ExecutorService did not terminate.");
                    }
                }

            } catch (InterruptedException ie) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
    
            try {
                saveState();
            } catch (Exception e) {
                System.err.println("Failed to save state during shutdown: " + e.getMessage());
                e.printStackTrace();
            }
    
            cleanupResources();
            System.out.println("Shutdown hook completed.");
        }));
    }
    
    /**
     * The main method is the entry point of the program.
     * It parses the command line arguments, initializes the necessary components, and starts the application.
     *
     * @param args The command line arguments.
     */
    public static void main(String[] args) {
        try {
            if (args.length != 8) {
                System.out.println("Usage: java IndexStorageBarrel <id> <multicast-address> <multicast-port> <rmi-registry-port> <ip host> <ip other machine> <gateway IP> <gateway RMI port>");
                return;
            }
    
            String id = args[0];
            String multicastAddress = args[1];
            int port = Integer.parseInt(args[2]);
            int registryPort = Integer.parseInt(args[3]);
            String rmiBindAddress = args[4];
            String remoteIpAddress = args[5];
            String gatewayIp = args[6];
            int gatewayPort = Integer.parseInt(args[7]);
    
            System.setProperty("java.rmi.server.hostname", rmiBindAddress);
    
            IndexStorageBarrel barrel = new IndexStorageBarrel(id, multicastAddress, port, rmiBindAddress, remoteIpAddress);

            Registry gatewayRegistry = LocateRegistry.getRegistry(gatewayIp, gatewayPort);
            GatewayInterface gatewayStub = (GatewayInterface) gatewayRegistry.lookup("GatewayService");
    
            Registry registry = LocateRegistry.createRegistry(registryPort);
            registry.rebind("IndexStorageBarrelService_" + id, barrel);
    
            barrel.addShutdownHook(gatewayStub);
    
            new Thread(barrel).start();
            System.out.println("Index Storage Barrel RMI Service with ID " + id + " is ready at " + multicastAddress + ":" + port + " with RMI registry on port " + registryPort);
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
}
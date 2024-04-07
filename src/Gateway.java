package src;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * The Gateway class serves as a central coordinator in a distributed indexing system.
 * It maintains a registry of IndexStorageBarrel instances, manages URL distribution for indexing,
 * and keeps track of system status and search frequencies.
 */
public class Gateway extends UnicastRemoteObject implements GatewayInterface {

    private BlockingQueue<URLTask> urlQueue;

    /** Tracks URLs that have been processed or are queued to prevent duplication. */
    private ConcurrentHashMap<String, Boolean> urlsSeen;

    /** List of registered barrels with their respective stubs for communication. */
    private List<BarrelData> barrels;

    /** Round-robin index to distribute tasks evenly across barrels. */
    private int nextBarrelIndex;

    /** Tracks the frequency of search terms across all barrels. */
    private final Map<String, Integer> searchFrequencies;

    /** Records the availability status of each barrel. */
    private final Map<String, Boolean> barrelStatus;

    /** Maintains response times for each barrel to monitor performance. */
    private final Map<String, List<Double>> barrelResponseTimes;

    /** ScheduledExecutorService to handle periodic tasks such as discovery of barrels. */
    private final ScheduledExecutorService scheduler;

    /** List of IP addresses for the Index Storage Barrels (ISB) machines. */
    private List<String> isbMachineIps;

    // This map holds URLs that are currently being processed.
    private ConcurrentHashMap<String, Boolean> urlsInProcess;


    private ConcurrentHashMap<String, Integer> urlOrigins; // New field to track origins of URLs




    

    /**
     * Creates a Gateway object that initializes with a list of ISB machine IP addresses.
     * It sets up the necessary data structures and starts the periodic discovery of barrels.
     *
     * @param isbMachineIps The list of IP addresses for the ISB machines.
     * @throws RemoteException if a networking error occurs.
     */
    protected Gateway(List<String> isbMachineIps) throws RemoteException {
        super();
        this.isbMachineIps = isbMachineIps;
        this.urlQueue = new PriorityBlockingQueue<>();
        urlsSeen = new ConcurrentHashMap<>();
        barrels = new ArrayList<>();
        searchFrequencies = new ConcurrentHashMap<>();
        barrelStatus = new ConcurrentHashMap<>();
        barrelResponseTimes = new ConcurrentHashMap<>();
        scheduler = Executors.newScheduledThreadPool(1);

        urlsInProcess = new ConcurrentHashMap<>();
        // Inside your Gateway constructor
        this.urlOrigins = new ConcurrentHashMap<>();
        
        discoverBarrels();
        startPeriodicDiscovery();
    }
    
    /**
     * Submits a URL for indexing. If the URL has not been seen before, it is added to the queue.
     *
     * @param url The URL to be submitted for indexing.
     * @return A string indicating the status of the operation.
     * @throws RemoteException if a networking error occurs.
     */
    @Override
    public String remoteSubmitURL(String url) throws RemoteException {
        synchronized (this) {
            if (urlsSeen.putIfAbsent(url, Boolean.TRUE) == null) {
                boolean isAdded = urlQueue.offer(new URLTask(url, 0)); // Priority 0 for user URLs
                if (isAdded) {
                    notifyDownloader();
                    urlOrigins.put(url, 0); // Mark as user-submitted
                    return "User-submitted URL enqueued with high priority: " + url;
                } else {
                    return "Failed to enqueue user URL: " + url;
                }
            } else {
                return "User URL already seen, skipping: " + url;
            }
        }
    }

    /**
     * Retrieves the next URL from the queue for processing.
     *
     * @return The next URL from the queue, or null if the queue is empty.
     * @throws RemoteException if a networking error occurs.
     */
    @Override
    public URLTask fetchNextUrl() throws RemoteException {
        synchronized (urlQueue) {
            URLTask task = urlQueue.poll(); // Retrieves and removes the head of the queue
            if (task != null) {
                String url = task.getUrl();
                urlsInProcess.put(url, Boolean.TRUE);
                return task;
            }
            return null;
        }
    }

    @Override
    public void markUrlAsProcessed(String url) {
        urlsInProcess.remove(url);
        // Optionally, add to a "completed" list to prevent re-processing in future.
    }

    
    /**
     * Searches for pages containing the specified terms.
     * This method distributes the search query to the next available barrel.
     *
     * @param terms Array of search terms.
     * @return A string representing the search results, or an error message if no barrels are available or an error occurs.
     * @throws RemoteException if a networking error occurs.
     */
    @Override
    public List<PageInfo> remoteSearchForTerms(String[] terms) throws RemoteException {
        try {
            if (terms == null || terms.length == 0) {
                System.out.println("No search terms provided");
                return null;
            }

            System.out.println("Search terms received: " + Arrays.toString(terms));

            BarrelData barrelData = getNextBarrel();
            if (barrelData == null) {
                System.out.println("No Index Storage Barrels are available.");
                return null;
            }
            IndexStorageBarrelInterface barrelStub = barrelData.stub;

            List<PageInfo> result = barrelStub.searchPagesWithTerms(terms);

            return result;
        } catch (ArrayIndexOutOfBoundsException ex) {
            ex.printStackTrace();
            System.out.println("Search failed due to an out-of-bounds access.");
            return null;
        } catch (Exception e) {
            System.err.println("Error in remoteSearchForTerms: " + e.getMessage());
            e.printStackTrace();
            System.out.println("Search failed due to an unexpected error.");
            return null;
        }
    }



    /**
     * Notifies the Gateway that a specific Barrel is now inactive.
     * The status of the barrel is updated to inactive and it is removed from the list of barrels.
     *
     * @param barrelID The ID of the barrel which has become inactive.
     * @throws RemoteException if a networking error occurs.
     */
    @Override
    public void notifyBarrelInactive(String barrelID) throws RemoteException {
        System.out.println("Barrel " + barrelID + " is now inactive.");
        updateBarrelStatus(barrelID, false);

        // Remove the barrel from the barrels list
        barrels.removeIf(barrelData -> barrelData.id.equals(barrelID));

        // Printing the status of all barrels
        System.out.println("Current Barrel Status:");
        barrelStatus.forEach((id, status) -> System.out.println("Barrel ID: " + id + ", Active: " + status));

        // Printing the list of barrels
        System.out.println("Current list of Barrels:");
        barrels.forEach(barrel -> System.out.println("Barrel ID: " + barrel.id));
    }


    
    /**
     * Retrieves the inbound links pointing to a specific URL.
     * This method forwards the request to the next available barrel.
     *
     * @param url The URL for which inbound links are requested.
     * @return A string containing the inbound links or an error message if no barrels are available or an error occurs.
     * @throws RemoteException if a networking error occurs.
     */
    @Override
    public String remoteGetLinksToPointingTo(String url) throws RemoteException {
        try {
            BarrelData barrelData = getNextBarrel();
            if (barrelData == null) {
                return "No Index Storage Barrels are available.";
            }
            IndexStorageBarrelInterface barrelStub = barrelData.stub;

            return barrelStub.getInboundLinks(url);
        } catch (Exception e) {
            System.err.println("Gateway exception when fetching inbound links: " + e.toString());
            e.printStackTrace();
            return "Failed to retrieve inbound links for URL: " + url;
        }
    }

    


/**
 * Adds a URL to the queue for indexing if it is not already present.
 * This method also notifies the downloader threads once the URL is added.
 *
 * @param url The URL to be added to the queue.
 * @throws RemoteException if a networking error occurs.
 */
    @Override
    public void addUrlToQueue(String url, int priority) throws RemoteException {
    synchronized (this) {
        if (!urlsSeen.containsKey(url)) {
            boolean isAdded = urlQueue.offer(new URLTask(url, priority));
            if (isAdded) {
                notifyDownloader();
                System.out.println("Outbound link added to queue with priority " + priority + ": " + url);
            } else {
                System.err.println("Failed to add outbound link to queue: " + url);
            }
            urlsSeen.put(url, Boolean.TRUE);
        } else {
            System.out.println("Outbound link already seen, skipping: " + url);
        }
    }
}



    /**
     * Retrieves the current status of the system, including information about top searches across all barrels
     * and the average response times of active barrels.
     * 
     * The method compiles and returns a detailed status report, which includes:
     * - The top 10 search terms used across all barrels, sorted by their frequency.
     * - The active status of each barrel and its average response time.
     *
     * @return A string representation of the system status.
     * @throws RemoteException if a networking error occurs.
     */
    @Override
    public String getSystemStatus() throws RemoteException {
        System.out.println("|UPDATE 15:05|");
        StringBuilder statusBuilder = new StringBuilder();

        statusBuilder.append("Top Searches Across All Barrels:\n");
        // Aggregate the top searches across all barrels
        Map<String, Integer> globalSearchFrequencies = new HashMap<>();
        for (BarrelData barrel : barrels) {
            Map<String, Integer> barrelSearchFrequencies = barrel.stub.getSearchFrequencies();
            // Combine frequencies from this barrel with the global frequencies
            barrelSearchFrequencies.forEach(
                (term, frequency) -> globalSearchFrequencies.merge(term, frequency, Integer::sum)
            );
        }
        // Now sort and get the top 10 searches globally
        List<String> topSearches = globalSearchFrequencies.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        topSearches.forEach(search -> statusBuilder.append(search).append("\n"));
        
        statusBuilder.append("\nActive Barrels with Average Response Times (in tenths of a second):\n");
        for (BarrelData barrel : barrels) {
            boolean isActive = barrelStatus.getOrDefault(barrel.id, false);
            if (isActive) {
                double averageTime = barrel.stub.getAverageResponseTime();
                statusBuilder.append("Barrel ").append(barrel.id)
                    .append(": ").append(String.format("%.1f", averageTime))
                    .append("\n");
            }
        }
        
        return statusBuilder.toString();
    }




   

    /**
     * Prints the current content of the URL queue to the console.
     * Each URL in the queue is listed in the order they were enqueued.
     */
    public void printQueueContent() {
        System.out.println("Current URLs in the Queue:");
        for (URLTask task : urlQueue) {
            System.out.println(task.getUrl() + " with priority " + task.getPriority());
        }
    }

    /**
     * Waits for a URL to be present in the queue. This method blocks until a URL becomes available.
     *
     * @throws InterruptedException if the thread is interrupted while waiting.
     */
    public synchronized void waitForUrl() throws InterruptedException{
        while (urlQueue.isEmpty()) {
            wait();
        }
    }
    

    /**
     * Notifies all waiting threads that a URL has been added to the queue.
     * Typically used to wake up threads waiting to process URLs.
     */
    public synchronized void notifyDownloader() {
        notifyAll();
    }



    /**
     * Retrieves the next available BarrelData object in a round-robin fashion.
     * If there are no connected barrels, this method returns null.
     *
     * @return The next BarrelData object, or null if no barrels are connected.
     */
    private BarrelData getNextBarrel() {
        if (barrels.isEmpty()) {
            System.err.println("No Index Storage Barrels are connected.");
            return null; 
        }
        BarrelData barrelData = barrels.get(nextBarrelIndex);
        nextBarrelIndex = (nextBarrelIndex + 1) % barrels.size();
        return barrelData;
    }
    
    /**
     * Records the frequency of a search term.
     * Each call increments the count of the given term.
     *
     * @param searchTerm The search term to record.
     */
    public void recordSearch(String searchTerm) {
        searchFrequencies.merge(searchTerm, 1, Integer::sum);
    }

    /**
     * Retrieves a list of the top 10 most frequently searched terms.
     *
     * @return A list of the top 10 search terms, sorted by frequency in descending order.
     */
    public List<String> getTopSearches() {
        // Return a list of the top 10 searches sorted by count
        return searchFrequencies.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Updates the status of a specific barrel.
     *
     * @param barrelID The ID of the barrel to update.
     * @param isActive The new status of the barrel; true if active, false if inactive.
     */
    public void updateBarrelStatus(String barrelID, boolean isActive) {
        barrelStatus.put(barrelID, isActive);
    }

    /**
     * Records the response time of a barrel after a search operation.
     *
     * @param barrelID The ID of the barrel for which the response time is recorded.
     * @param responseTime The response time to be recorded.
     */
    public void recordBarrelResponseTime(String barrelID, double responseTime) {
        barrelResponseTimes.computeIfAbsent(barrelID, k -> new ArrayList<>()).add(responseTime);
    }

    /**
     * Calculates the average response time of a barrel.
     *
     * @param barrelID The ID of the barrel for which the average response time is calculated.
     * @return The average response time of the specified barrel, or NaN if no data is available.
     */
    public double calculateAverageResponseTime(String barrelID) {
        List<Double> responseTimes = barrelResponseTimes.get(barrelID);
        return responseTimes.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    /**
     * Starts a periodic discovery process to detect and connect to Index Storage Barrels.
     * This method schedules a recurring task that runs the discovery process every 3 seconds.
     */
    private void startPeriodicDiscovery() {
    scheduler.scheduleAtFixedRate(() -> discoverBarrels(), 0, 3, TimeUnit.SECONDS);
    }

    /**
     * Discovers available Index Storage Barrels in the specified IP address range.
     * This method checks each IP address and port range for barrels and connects to new barrels that are not already in the list.
     */
    private void discoverBarrels() {
        for (String ip : isbMachineIps) {
            for (int port = 1100; port <= 1105; port++) {
                try {
                    Registry registry = LocateRegistry.getRegistry(ip, port);
                    String[] boundNames = registry.list();
                    for (String name : boundNames) {
                        if (name.startsWith("IndexStorageBarrelService_")) {
                            String id = name.substring(name.lastIndexOf('_') + 1);
                            if (!isAlreadyAdded(id)) {
                                try {
                                    IndexStorageBarrelInterface barrelStub = (IndexStorageBarrelInterface) registry.lookup(name);
                                    barrels.add(new BarrelData(id, barrelStub));
                                    updateBarrelStatus(id, true);
                                    System.out.println("Discovered and connected to barrel with ID: " + id + " at " + ip + ":" + port);
                                } catch (Exception e) {
                                    System.err.println("Failed to connect to barrel service: " + name + " at " + ip + ":" + port);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Could not connect to registry at " + ip + ":" + port);
                }
            }
        }
    }
    
    /**
     * Checks if a barrel with the specified ID has already been added to the list of barrels.
     *
     * @param id The ID of the barrel to check.
     * @return True if the barrel is already added, false otherwise.
     */
    private boolean isAlreadyAdded(String id) {
        return barrels.stream().anyMatch(barrel -> barrel.id.equals(id));
    }

    /**
     * The main method to start the Gateway server.
     * This method initializes the Gateway with the required configurations and starts the RMI registry.
     * 
     * @param args Command line arguments. 
     *             The first argument should be the RMI Registry port number.
     *             The second argument should be the IP address of the Gateway.
     *             The remaining arguments should be the IP addresses of the Index Storage Barrel (ISB) machines.
     * 
     * Usage:
     * java Gateway RMI_Registry_Port Gateway_IP_Address> ISB_Machine_IP1 [...ISB_Machine_IPn]
     * 
     * Example:
     * java Gateway 1099 192.168.1.2 192.168.1.3 192.168.1.4
     * 
     * This method will exit if the necessary arguments are not provided.
     * It sets the 'java.rmi.server.hostname' property to ensure RMI uses the correct network interface.
     * The 'sun.rmi.transport.tcp.responseTimeout' is also set for handling RMI response timeouts.
     * 
     * If an exception occurs during the initialization or binding process, it will be printed to the standard error.
     */
    public static void main(String[] args) {
        // Validate arguments: Expecting at least 3 arguments
        if (args.length < 3) {
            System.err.println("Error: You must provide the RMI Registry port number, the Gateway IP address, and at least one ISB machine IP address as command line arguments.");
            System.err.println("Usage: java Gateway <RMI_Registry_Port> <Gateway_IP_Address> <ISB_Machine_IP1> [...<ISB_Machine_IPn>]");
            System.exit(1);
        }

        int registryPort = Integer.parseInt(args[0]);
        String gatewayIP = args[1]; // Gateway IP address

        System.setProperty("sun.rmi.transport.tcp.responseTimeout", "120000");

        // Remaining arguments are the IPs of the ISB machines
        List<String> isbMachineIps = Arrays.asList(Arrays.copyOfRange(args, 2, args.length));

        // Set the system property for RMI to use the Gateway IP address
        System.setProperty("java.rmi.server.hostname", gatewayIP);
        
        try {
            
            Gateway server = new Gateway(isbMachineIps);
            Registry registry = LocateRegistry.createRegistry(registryPort); // Use the provided port for RMI registry
            registry.rebind("GatewayService", server);
            System.out.println("Gateway is ready on port " + registryPort + ".");
        } catch (Exception e) {
            System.err.println("Gateway exception: " + e.toString());
            e.printStackTrace();
        }
    }

}

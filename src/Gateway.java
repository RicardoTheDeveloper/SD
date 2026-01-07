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

public class Gateway extends UnicastRemoteObject implements GatewayInterface {

    private ConcurrentLinkedQueue<String> urlQueue; // Queue de URLs
    
    private List<BarrelData> barrels; // List of barrel stubs with their IDs

    private int nextBarrelIndex = 0; // Index for round-robin
    private final Map<String, Integer> searchFrequencies = new ConcurrentHashMap<>();
    // Fields to keep track of barrels and their response times
    private final Map<String, Boolean> barrelStatus = new ConcurrentHashMap<>();
    private final Map<String, List<Double>> barrelResponseTimes = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private List<String> isbMachineIps; // List to store the IPs of machines running ISBs

    protected Gateway(List<String> isbMachineIps) throws RemoteException {
        super();
        this.isbMachineIps = isbMachineIps; // Initialize with provided IPs
        urlQueue = new ConcurrentLinkedQueue<>();
        barrels = new ArrayList<>();
        discoverBarrels(1100, 1110); // Check ports 1100 to 1200 for barrels
        startPeriodicDiscovery();
        
    }

    
    
    

    @Override
    public synchronized void remoteSubmitURL(String url) throws RemoteException {
        // Meter o URL na queue
        boolean isAdded = urlQueue.offer(url);
        if (isAdded) {
            notifyDownloader(); // Notify downloader threads
            System.out.println("URL enqueued: " + url);
            System.out.println("DEPOIS DE ADICIONAR");
            printQueueContent();

        } else {
            // print de debugging
            System.err.println("Failed to enqueue URL: " + url);
        }
    }

    @Override
    public synchronized String fetchNextUrl() throws RemoteException {
        synchronized(urlQueue) {
            String url = urlQueue.poll();
            System.out.println("DEPOIS DE DAR FETCH");
            printQueueContent();
            return url; // Retrieve and remove the head of the queue
        }
    }

    @Override
    public void remoteRecursiveIndexUrls() throws RemoteException {
        // Logic for recursive indexing
        System.out.println("Recursive indexing requested.");
    }

    @Override
    public String remoteSearchForTerms(String[] terms) throws RemoteException {
        try {
            if (terms == null || terms.length == 0) {
                return "No search terms provided.";
            }

            System.out.println("|UPDATE| Search terms received: " + Arrays.toString(terms));
            
            BarrelData barrelData = getNextBarrel();  // Correctly obtain the next Barrel's data
            if (barrelData == null) {
                return "No Index Storage Barrels are available.";
            }
            IndexStorageBarrelInterface barrelStub = barrelData.stub;

            /*
             * 
             * for (String term : terms) {
                recordSearch(term);
            }
             */
            

            String result = barrelStub.searchPagesWithTerms(terms);

            
            //recordBarrelResponseTime(barrelData.id, responseTime);  // Use the Barrel's ID from BarrelData

            return result;
        } catch (ArrayIndexOutOfBoundsException ex) {
            ex.printStackTrace();
            return "Search failed due to an out-of-bounds access.";
        } catch (Exception e) {
            System.err.println("Error in remoteSearchForTerms: " + e.getMessage());
            e.printStackTrace(); // Will provide the full stack trace
            return "Search failed due to an unexpected error.";
        }
    }



    

    @Override
    public String remoteGetLinksToPointingTo(String url) throws RemoteException {
        try {
            BarrelData barrelData = getNextBarrel();  // Obtain the next Barrel's data
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

    //Tá okay meoo?



    @Override
    public String remoteGetRealTimeSystemStatus() throws RemoteException {
        // Logic to get real-time system status
        System.out.println("Real-time system status requested.");
        // Placeholder for system status
        return "System status: OK";
    }

    @Override
    public String remoteCheckInvertedIndex() throws RemoteException{
        System.out.println("Checking inverted index! (2)");
        return "print_index";
    }


    @Override
    public synchronized void addUrlToQueue(String url) throws RemoteException {
        if (!urlQueue.contains(url)) {
            boolean isAdded = urlQueue.offer(url);
            if (isAdded) {
                notifyDownloader(); // Notify downloader threads
                System.out.println("URL added to queue: " + url);
            } else {
                // Debug print in case of failure
                System.err.println("Failed to add URL to queue: " + url);
            }
        } else {
            // Debug print if the URL is already in the queue
            System.out.println("URL already in queue, skipping: " + url);
        }
    }

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




   


    public void printQueueContent() {
        System.out.println("Current URLs in the Queue:");
        for (String url : urlQueue) {
            System.out.println(url);
        }
    }


    public synchronized void waitForUrl() throws InterruptedException{
        while (urlQueue.isEmpty()) {
            wait();
        }
    }
    
    public synchronized void notifyDownloader() {
        notifyAll();
    }


    private BarrelData getNextBarrel() {
        if (barrels.isEmpty()) {
            System.err.println("No Index Storage Barrels are connected.");
            return null; // or handle this case as you see fit
        }
        BarrelData barrelData = barrels.get(nextBarrelIndex);
        nextBarrelIndex = (nextBarrelIndex + 1) % barrels.size();
        return barrelData;
    }
    
    

    


    // Record a search term
    public void recordSearch(String searchTerm) {
        searchFrequencies.merge(searchTerm, 1, Integer::sum);
    }

    // Method to get the top 10 search terms
    public List<String> getTopSearches() {
        // Return a list of the top 10 searches sorted by count
        return searchFrequencies.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // Update the barrel's status (you'll call this when barrels become active or inactive)
    public void updateBarrelStatus(String barrelID, boolean isActive) {
        barrelStatus.put(barrelID, isActive);
    }

    // Record the response time for a barrel (you'll call this after a search is done)
    public void recordBarrelResponseTime(String barrelID, double responseTime) {
        barrelResponseTimes.computeIfAbsent(barrelID, k -> new ArrayList<>()).add(responseTime);
    }

    // Calculate the average response time for a barrel
    public double calculateAverageResponseTime(String barrelID) {
        List<Double> responseTimes = barrelResponseTimes.get(barrelID);
        return responseTimes.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }


        private void startPeriodicDiscovery() {
        scheduler.scheduleAtFixedRate(() -> discoverBarrels(1100, 1110), 0, 3, TimeUnit.SECONDS);
    }

    private void discoverBarrels(int startPort, int endPort) {
        List<String> ipsToCheck = new ArrayList<>(isbMachineIps);
        
    
        for (String ip : ipsToCheck) {
            for (int port = startPort; port <= endPort; port++) {
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
    

    private boolean isAlreadyAdded(String id) {
        return barrels.stream().anyMatch(barrel -> barrel.id.equals(id));
    }



    public static void main(String[] args) {
        // Validate arguments: Expecting at least 3 arguments
        if (args.length < 3) {
            System.err.println("Error: You must provide the RMI Registry port number, the Gateway IP address, and at least one ISB machine IP address as command line arguments.");
            System.err.println("Usage: java Gateway <RMI_Registry_Port> <Gateway_IP_Address> <ISB_Machine_IP1> [...<ISB_Machine_IPn>]");
            System.exit(1);
        }
    
        int registryPort = Integer.parseInt(args[0]);
        String gatewayIP = args[1]; // Gateway IP address
    
        // Remaining arguments are the IPs of the ISB machines
        List<String> isbMachineIps = Arrays.asList(Arrays.copyOfRange(args, 2, args.length));
    
        // Set the system property for RMI to use the Gateway IP address
        System.setProperty("java.rmi.server.hostname", gatewayIP);
    
        try {
            Gateway server = new Gateway(isbMachineIps); // Pass ISB IPs to the Gateway constructor
            Registry registry = LocateRegistry.createRegistry(registryPort); // Create or access the registry
            registry.rebind("GatewayService", server); // Bind the Gateway service
            System.out.println("Gateway is ready on IP " + gatewayIP + " and port " + registryPort + ".");
        } catch (Exception e) {
            System.err.println("Gateway exception: " + e.toString());
            e.printStackTrace();
        }
    }
    
    

  
}
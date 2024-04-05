package src;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The RMIClient class represents a client program that connects to the RMI Registry and interacts with the system.
 * It provides a menu for the user to perform various actions such as indexing a new URL, searching pages with specific terms,
 * checking pages linking to a specific page, and getting real-time system status.
 * The program takes the server address and port number as command line arguments.
 */
public class RMIClient {

    private RMIClient() {}

    /**
     * The main method is the entry point of the RMIClient program.
     * It connects to the RMI Registry and provides a menu for the user to interact with the system.
     * The user can choose various actions such as indexing a new URL, searching pages with specific terms,
     * checking pages linking to a specific page, and getting real-time system status.
     * The program takes the server address and port number as command line arguments.
     * 
     * @param args the command line arguments containing the server address and port number
     */
    public static void main(String[] args) {

        if (args.length < 2) {
            System.err.println("Error: You must provide the server address and port number as command line arguments.");
            System.err.println("Usage: java RMIClient <Server_IP> <RMI_Registry_Port>");
            System.exit(1);
        }
    
        String serverAddress = args[0];
        int serverPort = Integer.parseInt(args[1]);

        try {

            System.out.println("Connecting to RMI Registry at " + serverAddress + ":" + serverPort);
            Registry registry = LocateRegistry.getRegistry(serverAddress, serverPort);
            GatewayInterface stub = (GatewayInterface) registry.lookup("GatewayService");

            Scanner scanner = new Scanner(System.in, "windows-1252"); 

            while (true) {
                
                System.out.println("Choose an action:");
                System.out.println("1. Index a new URL");
                System.out.println("2. Search pages with specific terms");
                System.out.println("3. Check pages linking to a specific page");
                System.out.println("4. Real-time system status");
                System.out.println("0. Exit");
                System.out.print("Enter your choice: ");
                int choice = scanner.nextInt();
                scanner.nextLine();

                switch (choice) {
                    case 1:
                        handleIndexNewURL(scanner, stub);
                        break;
                    case 2:
                        handleSearchWithSpecificTerms(scanner, stub);
                        break;
                    case 3:
                        handleCheckPagesLinkingTo(scanner, stub);
                        break;
                    case 4:
                        handleSystemStatus(stub);
                        break; 
                    case 0:
                        System.out.println("Exiting...");
                        scanner.close();
                        return;
                    default:
                        System.out.println("Invalid choice. Please enter a number between 0 and 6.");
                }
            }
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
    
    /**
     * Handles the process of submitting a new URL for indexing.
     * 
     * @param scanner the Scanner object used for user input
     * @param stub the GatewayInterface stub used for remote method invocation
     * @throws IOException if an I/O error occurs
     */
    private static void handleIndexNewURL(Scanner scanner, GatewayInterface stub) throws IOException {
        try {
            System.out.println("Enter a URL to be indexed:");
            String url = scanner.nextLine();
            stub.remoteSubmitURL(url);
            System.out.println("URL submitted for indexing: " + url);
        } catch (RemoteException e) {
            System.err.println("Remote exception when trying to submit URL for indexing.");
            e.printStackTrace();
        } 
    }

    /**
     * Handles the search with specific terms.
     * 
     * @param scanner the scanner object used for user input
     * @param stub the gateway interface stub used for remote method invocation
     */
    private static void handleSearchWithSpecificTerms(Scanner scanner, GatewayInterface stub) {
        try {
            System.out.print("Enter search terms (separated by spaces): ");
                String searchQuery = scanner.nextLine();
                String[] terms = searchQuery.split("\\s+");
                System.out.println("Term sent for search");
                for (String term : terms) {
                    System.out.println("Term: " + term);
                }
                String searchResults = stub.remoteSearchForTerms(terms);
                System.out.println("Search Results: \n" + searchResults);
        } catch (RemoteException e) {
            System.err.println("Error processing search request.");
            e.printStackTrace();
        }
    }

    /**
     * Handles the functionality to check for inbound links to a given URL.
     * 
     * @param scanner the Scanner object used to read user input
     * @param stub the GatewayInterface stub used for remote method invocation
     */
    private static void handleCheckPagesLinkingTo(Scanner scanner, GatewayInterface stub) {
        try {
            System.out.print("Enter the URL to check for inbound links: ");
            String url = scanner.nextLine();
            String inboundLinks = stub.remoteGetLinksToPointingTo(url);
            System.out.println("Inbound links for " + url + ": \n" + inboundLinks);
        } catch (RemoteException e) {
            System.err.println("Remote exception when trying to get inbound links.");
            e.printStackTrace();
        }
    }

    private static String lastSystemStatus = "";

    /**
     * Handles the monitoring of the system status.
     * 
     * This method continuously retrieves the system status from the provided GatewayInterface stub
     * and prints it to the console. It runs in a separate thread to allow for concurrent execution.
     * The monitoring can be stopped by typing 'exit' in the console.
     * 
     * @param stub The GatewayInterface stub used to retrieve the system status.
     */
    private static void handleSystemStatus(GatewayInterface stub) {

        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name());
        AtomicBoolean exitFlag = new AtomicBoolean(false);

        System.out.println("Monitoring system status. Type 'exit' to stop.");

        final boolean[] primeiraVezAux = new boolean[1];
        primeiraVezAux[0] = true;

        Thread statusThread = new Thread(() -> {
            while (!exitFlag.get()) {
                try {

                    String systemStatus = stub.getSystemStatus();
                    
                    if (!systemStatus.equals(lastSystemStatus) || (primeiraVezAux[0])) {
                        primeiraVezAux[0] = false;
                        System.out.println("System Status Updated: \n" + systemStatus);
                        lastSystemStatus = systemStatus;
                    }
                } catch (RemoteException e) {

                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    System.out.println("Monitoring interrupted.");
                    break;
                }
            }
        });

        statusThread.start();

        while (!"exit".equalsIgnoreCase(scanner.nextLine().trim())) {
            System.out.println("Type 'exit' to stop monitoring system status.");
        }

        exitFlag.set(true);
        statusThread.interrupt();
        try {
            statusThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Stopped monitoring system status.");
    }

}
// Remove the package declaration since the file is already in the correct package.

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class RMIClient {
    private RMIClient() {}

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Error: You must provide the server address and port number as command line arguments.");
            System.err.println("Usage: java RMIClient <Server_IP> <RMI_Registry_Port>");
            System.exit(1);
        }
    
        String serverAddress = args[0];
        int serverPort = Integer.parseInt(args[1]);


        try {
            System.out.println("Default Charset=" + Charset.defaultCharset());
            System.setProperty("file.encoding", "UTF-8");
            System.out.println("file.encoding=" + System.getProperty("file.encoding"));
            System.out.println("Default Charset=" + Charset.defaultCharset());
            System.out.println("Default Charset in Use=" + new OutputStreamWriter(new ByteArrayOutputStream()).getEncoding());



            
            // Creating the registry
            System.out.println("Connecting to RMI Registry at " + serverAddress + ":" + serverPort);
             // Creating the registry with provided server address and port.
            Registry registry = LocateRegistry.getRegistry(serverAddress, serverPort);
            GatewayInterface stub = (GatewayInterface) registry.lookup("GatewayService");

            Scanner scanner = new Scanner(System.in, "windows-1252"); 

            System.out.println("TESTE: NÃO, FAMÍLIA, PORTUGUÊS");

            while (true) {
                // Display the options menu
                System.out.println("Choose an action:");
                System.out.println("1. Index a new URL");
                System.out.println("2. Index all found URLs recursively");
                System.out.println("3. Search pages with specific terms");
                System.out.println("4. Search with results ordered by importance");
                System.out.println("5. Check pages linking to a specific page");
                System.out.println("6. Real-time system status");
                System.out.println("7. Check the inverted index");
                System.out.println("0. Exit");
                System.out.print("Enter your choice: ");
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume the newline

                switch (choice) {
                    case 1:
                        // Handle indexing a new URL
                        handleIndexNewURL(scanner, stub);
                        break;
                    case 2:
                        // Handle recursive indexing
                        System.out.println("Recursive indexing is not yet implemented.");
                        break;
                    case 3:
                        // Handle search with specific terms
                        handleSearchWithSpecificTerms(scanner, stub);
                        break;
                    case 4:
                        // Handle ordered search results
                        System.out.println("Ordered search results functionality is not yet implemented.");
                        break;
                    case 5:
                        // Handle checking pages linking to a specific page
                        handleCheckPagesLinkingTo(scanner, stub);
                        break;
                    case 6:
                        // Handle real-time system status
                        handleSystemStatus(stub);
                        break;

                    case 7:
                        //print para debugging de indexação
                        handleCheckingInvertedIndex(stub);
                        
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


    
private static void handleIndexNewURL(Scanner scanner, GatewayInterface stub) throws IOException {
    try {
        System.out.println("Enter a URL to be indexed:");
        String url = scanner.nextLine(); // Reading the URL
        stub.remoteSubmitURL(url);
        System.out.println("URL submitted for indexing: " + url);
    } catch (RemoteException e) {
        System.err.println("Remote exception when trying to submit URL for indexing.");
        e.printStackTrace();
    } 
}


private static void handleCheckingInvertedIndex(GatewayInterface stub){
    try{
        System.out.println("Checking the inverted index...");
        
        stub.remoteCheckInvertedIndex();

    }
    catch(RemoteException e){
        System.err.println("Remote exception when trying to check the inverted index");
        e.printStackTrace();

    }

}

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



private static void handleCheckPagesLinkingTo(Scanner scanner, GatewayInterface stub) {
    try {
        System.out.print("Enter the URL to check for inbound links: ");
        String url = scanner.nextLine(); // Read the URL from the user
        String inboundLinks = stub.remoteGetLinksToPointingTo(url); // Call the method to get inbound links
        System.out.println("Inbound links for " + url + ": \n" + inboundLinks);
    } catch (RemoteException e) {
        System.err.println("Remote exception when trying to get inbound links.");
        e.printStackTrace();
    }
}


private static void handleSystemStatus(GatewayInterface stub) {
    try {
        String systemStatus = stub.getSystemStatus();
        System.out.println("System Status: \n" + systemStatus);
    } catch (RemoteException e) {
        System.err.println("Remote exception when trying to get system status.");
        e.printStackTrace();
    }
}



}

package src;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class RMIClient {
    private RMIClient() {}

    public static void main(String[] args) {
        try {
            // Creating the registry
            Registry registry = LocateRegistry.getRegistry();
            GatewayInterface stub = (GatewayInterface) registry.lookup("GatewayService");

            Scanner scanner = new Scanner(System.in);
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
                        System.out.println("Search functionality is not yet implemented.");
                        break;
                    case 4:
                        // Handle ordered search results
                        System.out.println("Ordered search results functionality is not yet implemented.");
                        break;
                    case 5:
                        // Handle checking pages linking to a specific page
                        System.out.println("Link checking functionality is not yet implemented.");
                        break;
                    case 6:
                        // Handle real-time system status
                        System.out.println("Real-time system status functionality is not yet implemented.");
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


    private static void handleIndexNewURL(Scanner scanner, GatewayInterface stub) {
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


}

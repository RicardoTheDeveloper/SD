// Remove the package declaration since the file is already in the correct package.

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class RMIClient {
    private RMIClient() {}

    public static void main(String[] args) {
        try {
            // Assuming your RMI server is running on localhost and on the default port (1099)
            Registry registry = LocateRegistry.getRegistry();

            // Looking up the remote object by name "GatewayService"
            GatewayInterface stub = (GatewayInterface) registry.lookup("GatewayService");

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("Enter a URL to be indexed, or type 'exit' to quit:");
                String url = scanner.nextLine();
                
                if ("exit".equalsIgnoreCase(url)) {
                    break;
                }
                
                // Call the remote method submitUrl with a single URL
                stub.remoteSubmitURL(url);
                System.out.println("URL submitted for indexing: " + url);
            }
            scanner.close();
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}

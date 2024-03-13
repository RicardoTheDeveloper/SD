// Remove the package declaration since the file is already in the correct package.

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class RMIClient {
    private RMIClient() {}

    public static void main(String[] args) {
        try {
            //Criar registry
            Registry registry = LocateRegistry.getRegistry();

            
            GatewayInterface stub = (GatewayInterface) registry.lookup("GatewayService");

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("Enter a URL to be indexed, or type 'exit' to quit:");
                String url = scanner.nextLine();
                
                if ("exit".equalsIgnoreCase(url)) {
                    break;
                }
                
                
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

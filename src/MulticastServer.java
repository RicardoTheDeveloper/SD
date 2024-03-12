package src;

import java.net.MulticastSocket;
import java.util.Scanner;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.IOException;
import java.util.*;

public class MulticastServer extends Thread {
    private String MULTICAST_ADDRESS = "224.3.2.1";
    private int PORT = 4321;
    private long SLEEP_TIME = 5000;

    public static void main(String[] args) {
        MulticastServer server = new MulticastServer();
        server.start();
    }

    public MulticastServer() {
        super("Server " + (long) (Math.random() * 1000));
    }

    public void run() {
    MulticastSocket socket = null;
    long counter = 0;
    System.out.println(this.getName() + " running...");

    try (Scanner sc = new Scanner(System.in)) {
        while (true) {
            String texto = sc.nextLine();
            byte[] m = texto.getBytes();

            InetAddress aHost = InetAddress.getByName("localhost"); // Example: replace with appropriate address
            int serverPort = 1234; // Example: replace with appropriate port number
            DatagramPacket request = new DatagramPacket(m, m.length, aHost, serverPort);

            try (DatagramSocket aSocket = new DatagramSocket()) {
                aSocket.send(request);

                byte[] buffer = new byte[1000];
                DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
                aSocket.receive(reply);
                System.out.println("Received: " + new String(reply.getData(), 0, reply.getLength()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}

}

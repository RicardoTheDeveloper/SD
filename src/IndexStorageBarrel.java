package src;

import java.net.*;
import java.io.*;
import java.util.*;

public class IndexStorageBarrel extends Thread {
    private MulticastSocket socket;
    private InetAddress group;
    private int port = 4321; // Matching the port in the working example
    private Map<String, List<String>> index; // Data structure for storing words and URLs

    public IndexStorageBarrel(String multicastAddress, int port) throws IOException {
        this.port = port;
        this.group = InetAddress.getByName(multicastAddress);
        this.socket = new MulticastSocket(this.port);
        this.socket.joinGroup(this.group);
        this.index = new HashMap<>();
    }

    // Method to start listening for multicast messages
    public void start() {
        System.out.println("Index Storage Barrel is running...");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                System.out.println("ENTRAMOS");
                byte[] buffer = new byte[256];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                System.out.println("AQUI OK");
                socket.receive(packet);

                System.out.println("RECEBEMOS");

                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Recebemos a message:: "+message);
                processMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Method to process received messages and update the index
    private void processMessage(String message) {
        System.out.println("Received multicast message: " + message); // Add this line for debugging
        // Simplified processing logic
        String[] parts = message.split(";");
        if (parts.length >= 2) {
            String word = parts[0];
            String url = parts[1];
    
            index.computeIfAbsent(word, k -> new ArrayList<>()).add(url);
            System.out.println("Updated index with: " + word + " -> " + url);
        } else {
            System.out.println("Received malformed message: " + message);
        }
    }

    public static void main(String[] args) {
        try {
            IndexStorageBarrel barrel = new IndexStorageBarrel("224.3.2.1", 4321); // Use the same multicast address and port as MulticastClient
            barrel.start();
            
        } catch (IOException e) {
            System.out.println("Error starting Index Storage Barrel: " + e.getMessage());
        }
    }
}
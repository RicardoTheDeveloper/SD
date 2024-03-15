package src;

import java.net.*;
import java.io.*;
import java.util.*;

public class IndexStorageBarrel {
    private MulticastSocket socket;
    private InetAddress group;
    private int port;
    
    private Map<String, HashSet<String>> index; // Word to URLs
    private Map<String, PageInfo> urlDetails; // URL to PageInfo
    private List<String> messageQueue; // Message queue for the messages
    private MessageProcessor processor; // MessageProcessor thread

    public IndexStorageBarrel(String multicastAddress, int port) throws IOException {
        this.port = port;
        this.group = InetAddress.getByName(multicastAddress);
        this.socket = new MulticastSocket(this.port);
        this.socket.joinGroup(this.group);
        this.index = new HashMap<>();
        this.urlDetails = new HashMap<>();
        this.messageQueue = Collections.synchronizedList(new ArrayList<String>());
        this.processor = new MessageProcessor(this.index, this.urlDetails, this.messageQueue);
    }

    public void startListening() {
        System.out.println("Index Storage Barrel is running...");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] buffer = new byte[256];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                processMessage(message);

                
                printInvertedIndex();
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void printInvertedIndex() {
        System.out.println("PRINTING INDEX");
        synchronized (index) {
            for (Map.Entry<String, HashSet<String>> entry : index.entrySet()) {
                String word = entry.getKey();
                HashSet<String> urls = entry.getValue();
                System.out.println("Word: " + word);
                for (String url : urls) {
                    System.out.println(" - " + url);
                }
            }
        }
    }

    private void processMessage(String message) {
        String[] parts = message.split(" ; ");
        if (parts.length >= 2 && "type".equals(parts[0]) && "index".equals(parts[1])) {
            String key, value;
            for (int i = 2; i < parts.length; i += 2) {
                key = parts[i].toLowerCase();
                value = parts[i + 1];
    
                switch (key) {
                    case "word":
                        String url = parts[i + 3];
                        synchronized (index) {
                            index.computeIfAbsent(value, k -> new HashSet<>()).add(url);
                        }
                        i += 2; // Skip the next key-value pair as it's part of the current one
                        break;
                    case "title":
                    case "snippet":
                        // Additional processing for title and snippet if needed.
                        break;
                }
            }
        }
    }
    

    public static void main(String[] args) {
        try {
            IndexStorageBarrel barrel = new IndexStorageBarrel("224.3.2.1", 4321);
            barrel.startListening(); // Start listening for multicast messages
            
        } catch (IOException e) {
            System.out.println("Error starting Index Storage Barrel: " + e.getMessage());
        }
    }
}

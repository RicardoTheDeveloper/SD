package src;

import java.net.*;
import java.io.*;
import java.util.*;

public class IndexStorageBarrel extends Thread {
    private MulticastSocket socket;
    private InetAddress group;
    private int port = 4321; // Multicast port
    private Map<String, List<String>> index; // HashMap para URLs, de momento ainda está com string mas vai ser alterado

    public IndexStorageBarrel(String multicastAddress, int port) throws IOException {
        this.port = port;
        this.group = InetAddress.getByName(multicastAddress);
        this.socket = new MulticastSocket(this.port);
        this.socket.joinGroup(this.group);
        this.index = new HashMap<>();
    }

    // Começar a ouvir mensagens multicast
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

    // Método para processar a mensagem e atualizar o indice
    private void processMessage(String message) {
        System.out.println("Received multicast message: " + message); // print para debugging
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
            IndexStorageBarrel barrel = new IndexStorageBarrel("224.3.2.1", 4321); // usar o mesmo porto que o downloader
            barrel.start();
            
        } catch (IOException e) {
            System.out.println("Error starting Index Storage Barrel: " + e.getMessage());
        }
    }
}
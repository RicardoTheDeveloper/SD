package src;

import java.net.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IndexStorageBarrel implements Runnable {
    private MulticastSocket socket;
    private InetAddress group;
    private int port;

    private ConcurrentHashMap<String, PageInfo> pageInfoMap = new ConcurrentHashMap<>();
    private ExecutorService executorService;

    public IndexStorageBarrel(String multicastAddress, int port) throws IOException {
        this.port = port;
        this.group = InetAddress.getByName(multicastAddress);
        this.socket = new MulticastSocket(this.port);
        this.socket.joinGroup(this.group);
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Process the received packet in a separate thread
                executorService.submit(() -> processPacket(packet));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        executorService.shutdown();
    }

    private void processPacket(DatagramPacket packet) {
        try {
            ByteArrayInputStream byteStream = new ByteArrayInputStream(packet.getData());
            ObjectInputStream ois = new ObjectInputStream(byteStream);
            PageInfo pageInfo = (PageInfo) ois.readObject();

            pageInfoMap.put(pageInfo.getUrl(), pageInfo);
            printPageInfo(pageInfo); 

            savePageInfoToFile(pageInfo); // Save to file
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void printPageInfo(PageInfo pageInfo) {
        System.out.println("Received PageInfo:");
        System.out.println("URL: " + pageInfo.getUrl());
        System.out.println("Title: " + pageInfo.getTitle());
        System.out.println("Snippet: " + pageInfo.getSnippet());
        System.out.println("Words: " + pageInfo.getUniqueWords());
    }


    private void savePageInfoToFile(PageInfo pageInfo) {
        try (FileOutputStream fileOut = new FileOutputStream("pageInfoObjects.dat", true); // Append mode
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(pageInfo);
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            IndexStorageBarrel barrel = new IndexStorageBarrel("224.3.2.1", 4321);
            new Thread(barrel).start();
        } catch (IOException e) {
            System.out.println("Error starting Index Storage Barrel: " + e.getMessage());
        }
    }
}

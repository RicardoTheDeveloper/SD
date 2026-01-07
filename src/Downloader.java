package src;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.text.Normalizer;

public class Downloader implements Runnable {

    private static final AtomicInteger sharedCounter = new AtomicInteger(0);
    private final int threadId;
    private static int totalThreads = 100;
    private String multicastAddress = "224.3.2.1";
    private int port = 4321;
    private GatewayInterface gateway;
    private static final int MAX_UDP_PACKET_SIZE = 65507;
    private int nackPort;


    private final Map<String, Map<Integer, Long>> sentChunks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, byte[][]> originalDataChunks = new ConcurrentHashMap<>();


    public Downloader(GatewayInterface gateway, int threadId, int nackPort) {
        this.gateway = gateway;
        this.threadId = threadId;
        this.nackPort = nackPort;
    }


    

    

    // Método para enviar mensagens multicast (UDP!)


public void sendMulticastMessage(PageInfo pageInfo) {
    try (MulticastSocket socket = new MulticastSocket()) {
        InetAddress group = InetAddress.getByName(this.multicastAddress);

        // Serialize PageInfo object to byte array (não comprimido ainda)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(pageInfo);
        }
        byte[] pageInfoData = baos.toByteArray();

        // Calcula o número de chunks necessário
        int chunkSize = MAX_UDP_PACKET_SIZE - 100; // Deixa espaço para o cabeçalho
        int totalChunks = (pageInfoData.length + chunkSize - 1) / chunkSize;
        String messageId = "msg-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();


        byte[][] chunks = new byte[totalChunks][];
        originalDataChunks.put(messageId, chunks);



        for (int i = 0; i < totalChunks; i++) {
            int start = i * chunkSize;
            int length = Math.min(pageInfoData.length - start, chunkSize);
            byte[] chunkData = Arrays.copyOfRange(pageInfoData, start, start + length);

            // Comprime o chunk data
            ByteArrayOutputStream compressedBaos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(compressedBaos)) {
                gzipOutputStream.write(chunkData);
            }
            byte[] compressedChunkData = compressedBaos.toByteArray();
            chunks[i] = compressedChunkData; // Store the compressed chunk

            // Cria o objeto MessageChunk para este chunk
            MessageChunk chunk = new MessageChunk(i, totalChunks, compressedChunkData, messageId, this.nackPort, this.threadId);

            // Serialize o objeto MessageChunk para enviar
            ByteArrayOutputStream chunkBaos = new ByteArrayOutputStream();
            try (ObjectOutputStream chunkOos = new ObjectOutputStream(chunkBaos)) {
                chunkOos.writeObject(chunk);
            }
            byte[] chunkPacketData = chunkBaos.toByteArray();

            // Envia o chunk
            DatagramPacket packet = new DatagramPacket(chunkPacketData, chunkPacketData.length, group, this.port);
            socket.send(packet);
        }

    } catch (IOException e) {
        System.err.println("IOException: " + e.getMessage());
        e.printStackTrace();
    }
}




    private void listenForNackAndTerminationMessages() {
        try (DatagramSocket socket = new DatagramSocket(this.nackPort)) {
            byte[] buffer = new byte[MAX_UDP_PACKET_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (!Thread.currentThread().isInterrupted()) {
                socket.receive(packet);

                try (ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData());
                    ObjectInputStream ois = new ObjectInputStream(bais)) {
                    Object receivedObject = ois.readObject();

                    if (receivedObject instanceof NACKMessage) {
                        
                        NACKMessage nackMessage = (NACKMessage) receivedObject;
                        String messageId = nackMessage.getMessageId();
                        Set<Integer> missingChunks = nackMessage.getMissingChunks();

                        System.out.println("Thread " + this.threadId + " (NACK port " + this.nackPort + ") received NACK message for messageId: " + nackMessage.getMessageId());

                        byte[][] originalData = originalDataChunks.get(messageId);
                        if (originalData != null) {
                            for (Integer chunkIndex : missingChunks) {
                                if (chunkIndex != null && chunkIndex >= 0 && chunkIndex < originalData.length) {
                                    resendChunk(chunkIndex, messageId, originalData[chunkIndex]);
                                } else {
                                    System.err.println("Invalid chunk index or chunk data is null for index " + chunkIndex);
                                }
                            }
                        } else {
                            System.err.println("No original data found for message ID: " + messageId);
                        }
                    }
                    
                      else if (receivedObject instanceof TerminationMessage) {
                        TerminationMessage terminationMessage = (TerminationMessage) receivedObject;
                        if (terminationMessage.getThreadId() == this.threadId) {
                            System.out.println("Termination message received for thread " + this.threadId);
                            
                        }
                    }
                     
                    
                } catch (IOException | ClassNotFoundException e) {
                    System.err.println("Error handling the message: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error setting up the socket to listen for messages: " + e.getMessage());
        }
    }





    private void resendChunk(Integer chunkIndex, String messageId, byte[] chunkData) {
        try {
            
            // Serialize the chunk data along with any necessary headers or identifiers
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(new MessageChunk(chunkIndex, originalDataChunks.get(messageId).length, chunkData, messageId, nackPort, threadId));
            }
            byte[] dataToSend = baos.toByteArray();
            DatagramPacket resendPacket = new DatagramPacket(dataToSend, dataToSend.length, InetAddress.getByName(multicastAddress), port);
            try (MulticastSocket resendSocket = new MulticastSocket()) {
                resendSocket.send(resendPacket);
                System.out.println("Resent chunk " + chunkIndex + " for message " + messageId);
            }
        } catch (IOException e) {
            System.err.println("IOException while resending chunk: " + e.getMessage());
            e.printStackTrace();
        }
    }




    

    
    
    



    

    
    
    

    private String extractSnippet(Document doc) {
        Element firstParagraph = doc.selectFirst("p");
        return firstParagraph != null ? firstParagraph.text() : "";
    }

    


    public void handleIndexCommand(GatewayInterface gateway) {
    try {
        
        String currentUrl = gateway.fetchNextUrl();
        if (currentUrl != null && !currentUrl.isEmpty()) {
            Document doc = Jsoup.connect(currentUrl).get();
            String title = doc.title();
            String snippet = extractSnippet(doc);
            Set<String> outboundLinks = new HashSet<>();
            Set<String> uniqueWords = new HashSet<>();

            // Function to add normalized words to the set
            Consumer<String> addWord = word -> {
                word = word.toLowerCase().replaceAll("[^a-z]", "");
                if (!word.isEmpty() && !word.equals("www") && !word.equals("com")) {
                    uniqueWords.add(word);
                }
            };

            // Extract unique words from the title
            String[] titleWords = title.split("[\\s+.]+"); // Split by whitespace and periods
            for (String word : titleWords) {
                addWord.accept(word);
            }

            // Extract outbound links
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String foundUrl = link.absUrl("href");
                if (!foundUrl.isEmpty() && !foundUrl.equals(currentUrl)) {
                    outboundLinks.add(foundUrl);
                    gateway.addUrlToQueue(foundUrl);
                }
            }

            // Extract unique words from paragraphs
            Elements paragraphs = doc.select("p");
            for (Element paragraph : paragraphs) {
                String[] words = paragraph.text().split("\\s+");
                for (String word : words) {
                    addWord.accept(word);
                }
            }

            // Create PageInfo with only necessary information
            PageInfo pageInfo = new PageInfo(currentUrl, title, snippet, uniqueWords);
            pageInfo.setOutboundLinks(outboundLinks);
            sendMulticastMessage(pageInfo);
        }
    } catch (IOException e) {
        e.printStackTrace();
    }
}

    
    
    

    
    

    
    
    private String normalizeString(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("");
    }
    

    

    
    
    private void handleSearchCommand(String searchTerms) {
        // Logic to perform the search
    }

    private void handleCheckingInvertedIndex(String command){

    }


    


    @Override
    public void run() {
        
        Thread listenerThread = new Thread(this::listenForNackAndTerminationMessages);
        listenerThread.start();

        System.out.println("Thread " + threadId + " is running");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                synchronized (sharedCounter) {
                    while (sharedCounter.get() % totalThreads != threadId) {
                        sharedCounter.wait();
                    }

                    // It's this thread's turn
                    gateway.waitForUrl();
                    handleIndexCommand(gateway);

                    // Update the counter and notify all waiting threads
                    sharedCounter.set((sharedCounter.get() + 1) % totalThreads);
                    sharedCounter.notifyAll();
                }

                // Optional: If no URL, sleep before the next check
                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
            System.err.println("Downloader interrupted: " + e.toString());
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Error in Downloader: " + e.getMessage());
            e.printStackTrace();
        } // No need for finally block since we're not interrupting the listenerThread
    }


    

    public static void main(String[] args) {
        try {
            if (args.length < 3) {
                System.err.println("Usage: java Downloader <RMI_Host> <RMI_Port> <Base_NackPort>");
                return;
            }
        
            String rmiHost = args[0]; // RMI registry host
            int rmiPort = Integer.parseInt(args[1]); // RMI registry port
            int baseNackPort = Integer.parseInt(args[2]); // Starting port for NACK messages
        
            // Connect to RMI registry
            Registry registry = LocateRegistry.getRegistry(rmiHost, rmiPort);
            GatewayInterface gateway = (GatewayInterface) registry.lookup("GatewayService");
        
            int numberOfDownloaders = 100; // Or make this configurable as well
        
            for (int i = 0; i < numberOfDownloaders; i++) {
                int threadNackPort = baseNackPort + i; // Unique port for each thread
                Downloader downloader = new Downloader(gateway, i, threadNackPort);
                new Thread(downloader).start();
            }
        } catch (Exception e) {
            System.err.println("Downloader exception: " + e.toString());
            e.printStackTrace();
        }
    }
    
}
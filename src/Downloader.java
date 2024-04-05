package src;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
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

/**
 * The `Downloader` class implements the `Runnable` interface and represents a downloader
 * responsible for sending multicast messages and listening for NACK (Not Acknowledged) and
 * termination messages.
 * 
 * The `Downloader` class provides methods to send multicast messages containing page information
 * and to listen for NACK and termination messages on a datagram socket.
 * 
 * Usage:
 * 
 *     GatewayInterface gateway = new Gateway();
 *     int threadId = 1;
 *     int nackPort = 1234;
 *     Downloader downloader = new Downloader(gateway, threadId, nackPort);
 *     downloader.sendMulticastMessage(pageInfo);
 *     downloader.listenForNackAndTerminationMessages();
 * 
 */
public class Downloader implements Runnable {

    private static final AtomicInteger sharedCounter = new AtomicInteger(0);
    private final int threadId;
    private static int totalThreads = 100;
    private String multicastAddress = "224.3.2.1";
    private int port = 4321;
    private GatewayInterface gateway;
    private static final int MAX_UDP_PACKET_SIZE = 65507;
    private int nackPort;
    private final Map<String, byte[][]> originalDataChunks = new ConcurrentHashMap<>();

    /**
     * Constructs an instance of Downloader.
     * 
     * @param gateway   The Gateway interface that this Downloader communicates with.
     * @param threadId  The identifier of the thread being created.
     * @param nackPort  The port number used for NACK (Not Acknowledged) communications.
    */
    public Downloader(GatewayInterface gateway, int threadId, int nackPort) {
        this.gateway = gateway;
        this.threadId = threadId;
        this.nackPort = nackPort;
    }

    /**
     * Sends a multicast message containing the information of a page.
     * 
     * This method divides the page information into multiple chunks and sends each chunk
     * as a multicast packet. The chunks are compressed before sending to reduce the packet size.
     * The method automatically handles splitting the data into chunks that fit within the maximum
     * allowed size for UDP packets, performing compression on each chunk individually.
     * 
     * @param pageInfo A {@link PageInfo} object containing the information of the page to be sent.
     *                 This object will be serialized, divided into chunks, compressed, and then
     *                 transmitted via multicast.
     * 
     * <p>Sending process:
     * <ul>
     * <li>Serializes the {@link PageInfo} object.</li>
     * <li>Divides the serialized data into chunks based on the maximum UDP packet size.</li>
     * <li>Compresses each chunk using GZIP.</li>
     * <li>Sends each compressed chunk as a multicast packet to the defined group.</li>
     * </ul>
     * 
     * <p>IO (input/output) errors are caught and logged to the console.
    */
    public void sendMulticastMessage(PageInfo pageInfo) {

        try (MulticastSocket socket = new MulticastSocket()) {

            InetAddress group = InetAddress.getByName(this.multicastAddress);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(pageInfo);
            }

            byte[] pageInfoData = baos.toByteArray();
            int chunkSize = MAX_UDP_PACKET_SIZE - 100;
            int totalChunks = (pageInfoData.length + chunkSize - 1) / chunkSize;
            String messageId = "msg-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
            byte[][] chunks = new byte[totalChunks][];
            originalDataChunks.put(messageId, chunks);

            for (int i = 0; i < totalChunks; i++) {

                int start = i * chunkSize;
                int length = Math.min(pageInfoData.length - start, chunkSize);
                byte[] chunkData = Arrays.copyOfRange(pageInfoData, start, start + length);
                ByteArrayOutputStream compressedBaos = new ByteArrayOutputStream();

                try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(compressedBaos)) {
                    gzipOutputStream.write(chunkData);
                }

                byte[] compressedChunkData = compressedBaos.toByteArray();
                chunks[i] = compressedChunkData;

                MessageChunk chunk = new MessageChunk(i, totalChunks, compressedChunkData, messageId, this.nackPort, this.threadId);

                ByteArrayOutputStream chunkBaos = new ByteArrayOutputStream();
                try (ObjectOutputStream chunkOos = new ObjectOutputStream(chunkBaos)) {
                    chunkOos.writeObject(chunk);
                }

                byte[] chunkPacketData = chunkBaos.toByteArray();

                DatagramPacket packet = new DatagramPacket(chunkPacketData, chunkPacketData.length, group, this.port);
                socket.send(packet);

            }

        } 
        catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Listens for NACK (Not Acknowledged) and termination messages on a datagram socket.
     * 
     * This method starts a loop that continuously listens for NACK and termination messages sent
     * to the port specified by {@code this.nackPort}. Upon receiving a message, it checks the type
     * of the received object:
     * 
     * - For NACK messages, it identifies the missing data chunks and attempts to resend them.
     * - For termination messages, it checks if the message is intended for the current thread ID
     *   and, if so, performs the necessary actions to gracefully terminate the execution.
     * 
     * The method uses a {@link DatagramSocket} to receive UDP packets, and deserialization is employed
     * to reconstruct the message objects from the packet data.
     * 
     * <p>Exceptions during message reception or processing are caught and logged, but do not interrupt
     * the listening loop. The loop is only interrupted when the current thread is externally interrupted.
     * 
     * <p>Notes:
     * <ul>
     * <li>NACK messages contain the indices of the missing data chunks that need to be resent to ensure
     *    the complete delivery of the original message.</li>
     * <li>Termination messages are used to indicate to a thread that it should terminate its execution.</li>
     * </ul>
    */
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

    /**
     * Resends a specified data chunk to the multicast address.
     * 
     * This method is responsible for resending a data chunk that was not correctly received
     * by the recipient, as indicated by a received NACK message. The data chunk is encapsulated
     * in a {@link MessageChunk} object, which is then serialized and sent as a data packet via
     * multicast socket.
     * 
     * @param chunkIndex The index of the chunk within the original message being resent.
     * @param messageId The unique identifier of the original message to which the chunk belongs.
     * @param chunkData The data of the chunk to be resent.
     * 
     * <p>Resending process:
     * <ul>
     * <li>Serializes the {@link MessageChunk} object that contains the chunk index, chunk data,
     *     message ID, and other relevant information.</li>
     * <li>Packs the serialized data into a {@link DatagramPacket}.</li>
     * <li>Sends the packet to the configured multicast address.</li>
     * </ul>
     * 
     * <p>Errors during serialization or sending process are caught, logged to the console, and
     * detailed with a stack trace for easier debugging.
    */
    private void resendChunk(Integer chunkIndex, String messageId, byte[] chunkData) {
        try {
            
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

    /**
     * Extracts the text from the first paragraph of an HTML document.
     * 
     * This method uses the Jsoup library to parse the content of an HTML document and
     * extract the text contained in the first {@code <p>} paragraph. If the document does not
     * contain a paragraph, it returns an empty string.
     * 
     * @param doc The HTML document to be parsed, represented by a {@link Document} object
     *            from the Jsoup library.
     * @return The text from the first paragraph of the document or an empty string if no
     *         paragraph is found.
    */
    private String extractSnippet(Document doc) {
        Element firstParagraph = doc.selectFirst("p");
        return firstParagraph != null ? firstParagraph.text() : "";
    }


    /**
     * Handles the index command by fetching the next URL from the gateway, 
     * retrieving the webpage content, extracting relevant information, and 
     * sending a multicast message with the page information.
     * 
     * @param gateway the gateway interface used to fetch URLs and add URLs to the queue
     */
    public void handleIndexCommand(GatewayInterface gateway) {

        try {

            String currentUrl = gateway.fetchNextUrl();

            if (currentUrl != null && !currentUrl.isEmpty()) {

                Document doc = Jsoup.connect(currentUrl).get();
                String title = doc.title();
                String snippet = extractSnippet(doc);
                Set<String> outboundLinks = new HashSet<>();
                Set<String> uniqueWords = new HashSet<>();

                Consumer<String> addWord = word -> {
                    word = word.toLowerCase().replaceAll("[^a-z]", "");
                    if (!word.isEmpty() && !word.equals("www") && !word.equals("com")) {
                        uniqueWords.add(word);
                    }
                };

                String[] titleWords = title.split("[\\s+.]+");
                for (String word : titleWords) {
                    addWord.accept(word);
                }

                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    String foundUrl = link.absUrl("href");
                    if (!foundUrl.isEmpty() && !foundUrl.equals(currentUrl)) {
                        outboundLinks.add(foundUrl);
                        gateway.addUrlToQueue(foundUrl);
                    }
                }

                Elements paragraphs = doc.select("p");
                for (Element paragraph : paragraphs) {
                    String[] words = paragraph.text().split("\\s+");
                    for (String word : words) {
                        addWord.accept(word);
                    }
                }

                PageInfo pageInfo = new PageInfo(currentUrl, title, snippet, uniqueWords);
                pageInfo.setOutboundLinks(outboundLinks);
                sendMulticastMessage(pageInfo);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Executes the main logic of the Downloader thread.
     * This method is called when the thread is started.
     * It listens for Nack and Termination messages in a separate thread,
     * and then enters a loop where it waits for its turn to execute the download logic.
     * The download logic includes waiting for a URL from the gateway and handling the index command.
     * After executing the download logic, the shared counter is updated and other threads are notified.
     * The thread sleeps for 5000 milliseconds before starting the next iteration of the loop.
     * If the thread is interrupted, it prints an error message and interrupts itself.
     * If an IOException occurs during the download logic, it prints an error message and the stack trace.
     */
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

                    gateway.waitForUrl();
                    handleIndexCommand(gateway);

                    sharedCounter.set((sharedCounter.get() + 1) % totalThreads);
                    sharedCounter.notifyAll();
                }

                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
            System.err.println("Downloader interrupted: " + e.toString());
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("Error in Downloader: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * The main method is the entry point of the Downloader program.
     * It initializes the necessary variables and starts multiple downloader threads.
     * 
     * @param args The command line arguments. It should contain the following:
     *             - nackPort: The base port number for the downloader threads.
     *             - gatewayIP: The IP address of the gateway.
     *             - registryPort: The port number of the registry.
     */
    public static void main(String[] args) {
        try {
            if (args.length < 3) {
                System.err.println("Usage: java Downloader <nackPort> <gatewayIP> <registryPort>");
                return;
            }

            int baseNackPort = Integer.parseInt(args[0]);
            String gatewayIP = args[1];
            int registryPort = Integer.parseInt(args[2]);

            Registry registry = LocateRegistry.getRegistry(gatewayIP, registryPort);
            GatewayInterface gateway = (GatewayInterface) registry.lookup("GatewayService");

            int numberOfDownloaders = 100;
            for (int i = 0; i < numberOfDownloaders; i++) {
                int threadNackPort = baseNackPort + i;
                Downloader downloader = new Downloader(gateway, i, threadNackPort);
                new Thread(downloader).start();
            }
        } catch (Exception e) {
            System.err.println("Downloader exception: " + e.toString());
            e.printStackTrace();
        }
    }

}
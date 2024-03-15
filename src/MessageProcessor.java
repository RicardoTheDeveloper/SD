package src;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MessageProcessor extends Thread {
    private final Map<String, HashSet<String>> index;
    private final Map<String, PageInfo> urlDetails;
    private final List<String> messageQueue;

    public MessageProcessor(Map<String, HashSet<String>> index, Map<String, PageInfo> urlDetails, List<String> messageQueue) {
        this.index = index;
        this.urlDetails = urlDetails;
        this.messageQueue = messageQueue;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            String message;
            synchronized (messageQueue) {
                while (messageQueue.isEmpty()) {
                    try {
                        messageQueue.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                message = messageQueue.remove(0);
            }
            processMessage(message);
        }
    }

    private void processMessage(String message) {
        // Assuming message format: "word|URL|title|snippet"
        String[] parts = message.split("\\|");
        if (parts.length >= 4) {
            String word = parts[0].toLowerCase();
            String url = parts[1];
            String title = parts[2];
            String snippet = parts[3];

            
        synchronized (index) {
            index.computeIfAbsent(word, k -> new HashSet<>()).add(url);
        }

        // Update the URL details
        synchronized (urlDetails) {
            PageInfo pageInfo = new PageInfo(url, title, snippet);
            urlDetails.put(url, pageInfo);
        }


        } else {
            System.out.println("Received malformed message: " + message);
        }
    }
}

package src;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.StringTokenizer;



public class Downloader {
    private Queue<String> urlQueue;

    public Downloader() {
        urlQueue = new LinkedList<>();
    }

    public void processUrl(String url) {
        try {
            String content = fetchContent(url);
            Document doc = Jsoup.parse(content);

            Set<String> words = extractWords(doc);
            // Process words and associate them with the URL...

            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String newUrl = link.attr("abs:href");
                urlQueue.add(newUrl);
            }

            // Optionally, distribute some URLs to other downloaders
            distributeUrls();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void enqueueURL(String url) {
        // Add URL to the queue
        urlQueue.add(url);
        // You might want to start processing the queue here or have it run in a separate thread
    }

    private String fetchContent(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        connection.disconnect();

        return content.toString();
    }

    private Set<String> extractWords(Document doc) {
        // Implement logic to extract words from the doc
        return new HashSet<>();
    }

    private void distributeUrls() {
        // Implement logic to distribute URLs to other downloaders
    }

    // main method for testing
    public static void main(String[] args) {
        Downloader downloader = new Downloader();
        downloader.processUrl("http://example.com");
    }
}



import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Downloader {

    private String multicastAddress = "224.3.2.1"; // multicast address
    private int port = 4321; // multicast port

    

    // Método para enviar mensagens multicast (UDP!)
    public void sendMulticastMessage(String message) {
        MulticastSocket socket = null;
        try {
            System.out.println("ENTREI NO SEND");
            socket = new MulticastSocket(); // Criar Socket
            InetAddress group = InetAddress.getByName(this.multicastAddress);
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, this.port);
            socket.send(packet);
            
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    public static void main(String[] args) {
        try {
            // Conectar a RMI registry
            Registry registry = LocateRegistry.getRegistry("localhost", 1099); //registry RMI para comunicar com a gateway
            GatewayInterface gateway = (GatewayInterface) registry.lookup("GatewayService");
            Downloader downloader = new Downloader();



            while (true) {
                // Ir buscar o url ao gateway
                String url = gateway.fetchNextUrl();
                if (url != null) {
                    // Process the URL
                    try {
                        Document doc = Jsoup.connect(url).get();
                        
                        // Printar o titulo da página
                        System.out.println("Title: " + doc.title());


                        downloader.sendMulticastMessage(doc.title() + ";" + url); //enviar msg multicast para ISB
                        
                        // printar os parágrafos
                        Elements paragraphs = doc.select("p");
                        for (Element paragraph : paragraphs) {
                            System.out.println(paragraph.text());
                            downloader.sendMulticastMessage(paragraph.text() + ";" + url);
                        }

                        // Printar todos os links
                        Elements links = doc.select("a[href]");
                        for (Element link : links) {
                            System.out.println("Text: " + link.text());
                            System.out.println("URL: " + link.attr("abs:href"));
                        }
                        
                        
                        
                    } catch (IOException e) {
                        System.err.println("Error downloading content from: " + url);
                        e.printStackTrace();
                    }
                } else {
                    // Se não tivermos nenhum url, esperar 5 segundos antes de tentar outra vez
                    Thread.sleep(5000); // Sleep de 5 segundos
                }
            }
        } catch (Exception e) {
            System.err.println("Downloader exception: " + e.toString());
            e.printStackTrace();
        }
    }
}

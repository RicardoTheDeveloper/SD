package src;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class SimpleTest {
    public static void main(String[] args) {
        try {
            System.setProperty("file.encoding", "UTF-8");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            System.out.print("Digite algo: ");
            String input = reader.readLine();
            System.out.println("Você digitou: " + input);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package uppa.group2;

import io.github.cdimascio.dotenv.Dotenv;
import uppa.group2.client.ChatClient;

import java.io.IOException;
import java.util.Scanner;

public class ClientMain {
    static void main(String[] args) throws IOException {
        Dotenv dotenv = Dotenv.load();
        int SERVER_PORT = Integer.parseInt(dotenv.get("SERVER_PORT"));
        String SERVER_HOST = dotenv.get("SERVER_HOST");

        IO.println("Client is running !");

        ChatClient client = new ChatClient();
        client.connect(SERVER_HOST, SERVER_PORT);

        // Thread 1 — écoute les messages entrants
        new Thread(() -> {
            try {
                client.listenForMessages();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

        // Thread 2 (main) — lit ce que l'utilisateur tape
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            client.sendMessage(scanner.nextLine());
        }
    }
}

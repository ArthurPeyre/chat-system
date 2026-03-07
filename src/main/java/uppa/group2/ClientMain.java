package uppa.group2;

import uppa.group2.client.ChatClient;

import java.io.IOException;
import java.util.Scanner;

public class ClientMain {
    static void main(String[] args) throws IOException {
        IO.println("Client is running !");

        ChatClient client = new ChatClient();
        client.connect("localhost", 8080);

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

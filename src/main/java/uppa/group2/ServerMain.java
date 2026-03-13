package uppa.group2;

import io.github.cdimascio.dotenv.Dotenv;
import uppa.group2.server.ChatServer;

import java.io.IOException;

public class ServerMain {
    static void main(String[] args) throws IOException {
        Dotenv dotenv = Dotenv.load();
        int SERVER_PORT = Integer.parseInt(dotenv.get("SERVER_PORT"));

        ChatServer server = new ChatServer(SERVER_PORT);
        server.start();
    }
}

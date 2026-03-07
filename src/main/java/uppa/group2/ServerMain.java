package uppa.group2;

import uppa.group2.server.ChatServer;

import java.io.IOException;

public class ServerMain {
    static void main(String[] args) throws IOException {
        ChatServer server = new ChatServer(8080);
        server.start();
    }
}

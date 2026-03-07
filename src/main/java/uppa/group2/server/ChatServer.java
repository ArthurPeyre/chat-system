package uppa.group2.server;

import uppa.group2.entity.Message;
import uppa.group2.service.MessageService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {

    private final int port;
    private final List<ClientHandler> connectedClients = new CopyOnWriteArrayList<>();
    private final MessageService messageService = new MessageService();

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            IO.println("Serveur démarré sur le port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                ClientHandler handler = new ClientHandler(clientSocket, this);
                connectedClients.add(handler);

                new Thread(handler).start();
            }
        }
    }

    public void broadcast(Message message, ClientHandler sender) {
        connectedClients.stream()
                .filter(client -> client != sender)
                .forEach(client -> client.sendMessage(message));
    }

    public void removeClient(ClientHandler handler) {
        connectedClients.remove(handler);
    }

    public MessageService getMessageService() {
        return messageService;
    }
}

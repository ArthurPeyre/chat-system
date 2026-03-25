package uppa.group2.network;

import uppa.group2.controller.ChatController;
import uppa.group2.model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends Thread {

    private final int port;
    private final ChatController controller;
    private ServerSocket serverSocket;

    public Server(ChatController controller) {
        this.port = 5000;
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                new ConnectionHandler(clientSocket, controller).start();
            }
        } catch (IOException e) {
            if (!serverSocket.isClosed()) {
                e.printStackTrace();
            }
        }
    }

    public void stopServer() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private static class ConnectionHandler extends Thread {

        private final Socket socket;
        private final ChatController controller;

        public ConnectionHandler(Socket socket, ChatController controller) {
            this.socket = socket;
            this.controller = controller;
        }

        @Override
        public void run() {
            try (
                ObjectInputStream in  = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

                Object received = in.readObject();

                if (received instanceof Message message) {
                    switch (message.getType()) {
                        case CONNECT:
                            controller.handleConnect(message.getSender(), out);
                            break;

                        case DISCONNECT:
                            controller.handleDisconnect(message.getSender());
                            break;

                        case TEXT:
                            controller.handleMessage(message);
                            break;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }

}

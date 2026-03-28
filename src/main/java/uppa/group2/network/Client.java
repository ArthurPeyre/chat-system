package uppa.group2.network;

import uppa.group2.model.Message;
import uppa.group2.model.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client extends Thread {

    private final Message message;
    private final List<User> recipients;

    public Client(Message message, List<User> recipients) {
        this.message = message;
        this.recipients = recipients;
    }

    @Override
    public void run() {
        for (User recipient: recipients) {
            sendTo(recipient);
        }
    }

    private void sendTo(User recipient) {
        try (
            Socket socket = new Socket(recipient.getHost(), recipient.getPort());
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            out.flush();
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            System.err.println("Impossible de joindre " + recipient + " : " + e.getMessage());
        }
    }

    public static List<User> sendConnectAndGetUsers(User localUser, String host, int port) {
        List<User> users = new ArrayList<>();

        try (Socket socket = new Socket(host, port)) {
            System.out.println("=== Connexion TCP établie vers " + host + ":" + port);

            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.flush(); // ← important, flush avant de créer l'InputStream
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            Message connectMsg = new Message(localUser, List.of(), "", Message.Type.CONNECT);
            out.writeObject(connectMsg);
            out.flush();

            // socket.shutdownOutput();

            Object response = in.readObject();
            if (response instanceof List<?> list) {
                System.out.println("<<< Réception liste : " + list);
                for (Object obj : list) {
                    if (obj instanceof User user) {
                        users.add(user);
                    }
                }
            }

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Bootstrap échoué : " + e.getMessage());
        }

        return users;
    }

}

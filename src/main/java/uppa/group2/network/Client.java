package uppa.group2.network;

import uppa.group2.model.Message;
import uppa.group2.model.User;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
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
        ) {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            System.err.println("Impossible de joindre " + recipient + " : " + e.getMessage());
        }
    }

    private static void sendConnectAndGetUsers(User localUser, String userHost, int userPort) {

    }

}

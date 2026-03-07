package uppa.group2.server;

import uppa.group2.entity.Message;
import uppa.group2.entity.User;
import uppa.group2.viewmodel.MessageViewModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.UUID;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ChatServer server;
    private PrintWriter out;
    private User user;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Entrez votre pseudo: ");
            String username = in.readLine();
            user = new User(username);
            server.broadcast(buildMessage(username + " a rejoint le chat"), this);

            String rawMessage;
            while ((rawMessage = in.readLine()) != null) {
                Message message = buildMessage(rawMessage);
                server.getMessageService().save(message);
                server.broadcast(message, this);
            }
        } catch (IOException e) {
            IO.println("Client déconnecté: " + user.getUsername());
        } finally {
            server.removeClient(this);
        }
    }

    public void sendMessage(Message message) {
        // Transforme l'Entity en ViewModel avant d'envoyer
        MessageViewModel vm = MessageViewModel.from(message);
        out.println(vm.toJson());
    }

    private Message buildMessage(String content) {
        return new Message(UUID.randomUUID(), content, user, LocalDateTime.now());
    }

}

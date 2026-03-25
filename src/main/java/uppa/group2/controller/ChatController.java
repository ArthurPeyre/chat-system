package uppa.group2.controller;

import uppa.group2.model.Message;
import uppa.group2.model.User;
import uppa.group2.network.Client;
import uppa.group2.network.DiscoveryBroadcaster;
import uppa.group2.network.Server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatController {

    private final User localUser;
    private final CopyOnWriteArrayList<User> connectedUsers;
    private final ChatView view;
    private final Server server;

    public ChatController(User localUser, ChatView view) {
        this.localUser = localUser;
        this.connectedUsers = new CopyOnWriteArrayList<>();
        this.view = view;
        this.server = new Server(localUser.getPort(), this);
    }

    // -------------------------
    // Démarrage
    // -------------------------

    public void start() {
        server.start();
        new DiscoveryBroadcaster(localUser, 6000).start();
        view.showConnectedUsers(connectedUsers);
    }

    public void handleDiscovery(User user) {
        if (connectedUsers.contains(user) || user.equals(localUser)) return;

        List<User> users = Client.sendConnectAndGetUsers(localUser, user.getHost(), user.getPort());
        connectedUsers.addAll(users);
        broadcastSystemMessage(Message.Type.CONNECT);
        view.showConnectedUsers(connectedUsers);
    }


    // -------------------------
    // Handlers (appelés par Server)
    // -------------------------

    // Un nouveau pair se connecte : on lui renvoie la liste, on l'ajoute, on informe la view
    public void handleConnect(User user, ObjectOutputStream out) {
        try {
            out.writeObject(new ArrayList<>(connectedUsers));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        connectedUsers.add(user);
        view.showUserConnected(user);
        view.showConnectedUsers(connectedUsers);
    }

    // Un pair se déconnecte
    public void handleDisconnect(User user) {
        connectedUsers.remove(user);
        view.showUserDisconnected(user);
        view.showConnectedUsers(connectedUsers);
    }

    // Réception d'un message texte
    public void handleMessage(Message message) {
        view.showMessage(message);
    }

    // -------------------------
    // Actions utilisateur (appelées par la View)
    // -------------------------

    // Envoie un message à un ou plusieurs destinataires
    public void sendMessage(String content, List<User> recipients) {
        Message message = new Message(localUser, recipients, content, Message.Type.TEXT);
        new Client(message, recipients).start();
    }

    // Déconnexion propre
    public void disconnect() {
        broadcastSystemMessage(Message.Type.DISCONNECT);
        server.stopServer();
    }

    // -------------------------
    // Utilitaires
    // -------------------------

    private void broadcastSystemMessage(Message.Type type) {
        Message msg = new Message(localUser, new ArrayList<>(connectedUsers), "", type);
        new Client(msg, new ArrayList<>(connectedUsers)).start();
    }

    public List<User> getConnectedUsers() {
        return connectedUsers;
    }

    public User getLocalUser() {
        return localUser;
    }

}

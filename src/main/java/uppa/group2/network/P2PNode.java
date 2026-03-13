package uppa.group2.network;

import uppa.group2.util.Logger;
import uppa.group2.util.NetworkUtils;
import uppa.group2.core.MessageListener;
import uppa.group2.core.PeerRegistry;
import uppa.group2.model.Message;
import uppa.group2.model.Peer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class P2PNode {

    private final String username;
    private final String localHost;
    private final int localPort;
    private final PeerRegistry registry;
    private final List<MessageListener> listeners = new CopyOnWriteArrayList<>();

    // username -> connexion TCP active
    private final Map<String, PeerConnection> connections = new ConcurrentHashMap<>();

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private final ExecutorService connectionPool = Executors.newCachedThreadPool();
    private volatile boolean running = false;

    public P2PNode(String username, int port) throws IOException {
        this.username = username;
        this.localPort = port;
        this.localHost = NetworkUtils.getLocalHost();
        this.registry = new PeerRegistry();
    }

    // =========================================================================
    // Démarrage / Arrêt
    // =========================================================================

    /**
     * Démarre le serveur d'écoute.
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(localPort, 50, InetAddress.getByName("0.0.0.0"));
        running = true;
        Logger.info("Serveur démarré sur " + localHost + ":" + localPort);

        acceptThread = new Thread(this::acceptLoop, "Accept-Thread");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    /**
     * Arrête le nœud et ferme toutes les connexions.
     */
    public void stop() {
        running = false;

        // Notifier tous les pairs de notre départ
        broadcast(Message.createUserNotification(
                Message.Type.USER_LEAVE, username, localHost, localPort));

        // Fermer toutes les connexions
        connections.values().forEach(PeerConnection::close);
        connections.clear();
        registry.clear();

        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}

        connectionPool.shutdownNow();
        Logger.info("Nœud arrêté.");
    }

    // =========================================================================
    // Connexion à un pair (bootstrap)
    // =========================================================================

    /**
     * Se connecte à un pair existant pour rejoindre le réseau.
     * @param host  hôte du pair
     * @param port  port du pair
     */
    public void connectToPeer(String host, int port) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000);

        // Créer un pair temporaire pour la connexion bootstrap
        Peer bootstrapPeer = new Peer("__bootstrap__", host, port);

        PeerConnection conn = new PeerConnection(socket, bootstrapPeer,
                this::handleIncomingMessage, this::handleConnectionClosed);

        // Envoyer notre présence
        Message joinMsg = Message.createUserNotification(
                Message.Type.USER_JOIN, username, localHost, localPort);
        conn.send(joinMsg);

        // Démarrer l'écoute — le pair nous répondra avec USER_LIST
        // puis d'éventuels USER_JOIN d'autres pairs
        conn.startListening();

        // La connexion est stockée temporairement sous le nom bootstrap
        // Elle sera remplacée dès la réception du nom réel du pair
        connections.put("__bootstrap__@" + host + ":" + port, conn);

        Logger.info("Connecté au pair bootstrap " + host + ":" + port);
    }

    // =========================================================================
    // Boucle d'acceptation des connexions entrantes
    // =========================================================================

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                connectionPool.submit(() -> handleIncomingConnection(socket));
            } catch (IOException e) {
                if (running) Logger.error("Erreur acceptation: " + e.getMessage());
            }
        }
    }

    private void handleIncomingConnection(Socket socket) {
        try {
            String remoteHost = socket.getInetAddress().getHostAddress();
            Logger.info("Nouvelle connexion entrante depuis " + remoteHost);

            // Pair temporaire — le nom sera mis à jour dès le premier message USER_JOIN
            Peer tempPeer = new Peer("__incoming__@" + remoteHost, remoteHost,
                    socket.getPort());

            PeerConnection conn = new PeerConnection(socket, tempPeer,
                    this::handleIncomingMessage, this::handleConnectionClosed);
            conn.startListening();

            // Stockée temporairement
            connections.put(tempPeer.getUsername(), conn);

        } catch (IOException e) {
            Logger.error("Erreur création connexion entrante: " + e.getMessage());
        }
    }

    // =========================================================================
    // Gestion des messages entrants
    // =========================================================================

    private void handleIncomingMessage(Message msg) {
        switch (msg.getType()) {
            case USER_JOIN     -> handleUserJoin(msg);
            case USER_LEAVE    -> handleUserLeave(msg);
            case USER_LIST     -> handleUserList(msg);
            case USER_LIST_REQUEST -> handleUserListRequest(msg);
            case TEXT          -> handleTextMessage(msg);
            case FILE          -> handleFileMessage(msg);
            default            -> Logger.warn("Message de type inconnu: " + msg.getType());
        }
    }

    /**
     * Un pair annonce sa connexion.
     */
    private void handleUserJoin(Message msg) {
        String peerUsername = msg.getSenderUsername();
        String peerHost = msg.getSenderHost();
        int peerPort = msg.getSenderPort();

        if (peerUsername.equals(username)) return; // Ignorer notre propre echo

        // Retirer l'éventuelle connexion temporaire et la remplacer par la vraie
        // Trouver la connexion entrante correspondant à cet hôte
        PeerConnection existingConn = connections.values().stream()
                .filter(c -> c.getPeer().getHost().equals(peerHost)
                        && c.getPeer().getUsername().startsWith("__"))
                .findFirst()
                .orElse(null);

        if (existingConn != null) {
            // Mettre à jour le nom du pair dans notre registre
            String oldKey = existingConn.getPeer().getUsername();
            connections.remove(oldKey);
            existingConn.getPeer().setUsername(peerUsername);
            existingConn.getPeer().setPort(peerPort);
            connections.put(peerUsername, existingConn);
        } else if (!connections.containsKey(peerUsername)) {
            // Connexion sortante vers ce nouveau pair
            connectionPool.submit(() -> {
                try {
                    connectOutbound(peerUsername, peerHost, peerPort);
                } catch (IOException e) {
                    Logger.error("Impossible de se connecter à " + peerUsername + ": " + e.getMessage());
                }
            });
        }

        // Enregistrer le pair
        if (!registry.hasPeer(peerUsername)) {
            Peer newPeer = new Peer(peerUsername, peerHost, peerPort);
            registry.addPeer(newPeer);

            // Notifier l'interface utilisateur
            listeners.forEach(l -> l.onPeerConnected(newPeer));

            Logger.info("Pair connecté: " + peerUsername + " @ " + peerHost + ":" + peerPort);

            // Envoyer la liste de nos pairs connus au nouveau venu (si on a une connexion établie)
            sendUserList(peerUsername);
        }
    }

    /**
     * Un pair annonce sa déconnexion.
     */
    private void handleUserLeave(Message msg) {
        String peerUsername = msg.getSenderUsername();
        Peer peer = registry.getPeer(peerUsername);

        if (peer != null) {
            registry.removePeer(peerUsername);
            PeerConnection conn = connections.remove(peerUsername);
            if (conn != null) conn.close();

            listeners.forEach(l -> l.onPeerDisconnected(peer));
            Logger.info("Pair déconnecté: " + peerUsername);
        }
    }

    /**
     * Réception de la liste des pairs (envoyée par le bootstrap).
     */
    private void handleUserList(Message msg) {
        // Le contenu est sérialisé dans textContent au format "user@host:port;user@host:port;..."
        String listData = msg.getTextContent();
        if (listData == null || listData.isBlank()) return;

        List<Peer> peerList = new ArrayList<>();
        for (String entry : listData.split(";")) {
            if (entry.isBlank()) continue;
            try {
                String[] parts = entry.split("@");
                String peerUsername = parts[0];
                String[] hostPort = parts[1].split(":");
                String host = hostPort[0];
                int port = Integer.parseInt(hostPort[1]);

                if (!peerUsername.equals(username) && !registry.hasPeer(peerUsername)) {
                    Peer p = new Peer(peerUsername, host, port);
                    peerList.add(p);
                    registry.addPeer(p);

                    // Se connecter à ce pair
                    final String fu = peerUsername;
                    final String fh = host;
                    final int fp = port;
                    connectionPool.submit(() -> {
                        try {
                            connectOutbound(fu, fh, fp);
                        } catch (IOException e) {
                            Logger.error("Connexion échouée vers " + fu + ": " + e.getMessage());
                        }
                    });
                }
            } catch (Exception e) {
                Logger.warn("Entrée de liste invalide: " + entry);
            }
        }

        if (!peerList.isEmpty()) {
            listeners.forEach(l -> l.onPeerListReceived(peerList));
        }
    }

    /**
     * Un pair nous demande notre liste de pairs connus.
     */
    private void handleUserListRequest(Message msg) {
        sendUserList(msg.getSenderUsername());
    }

    private void sendUserList(String targetUsername) {
        PeerConnection conn = connections.get(targetUsername);
        if (conn == null) return;

        String listData = registry.getAllPeers().stream()
                .filter(p -> !p.getUsername().equals(targetUsername))
                .map(p -> p.getUsername() + "@" + p.getHost() + ":" + p.getPort())
                .collect(Collectors.joining(";"));

        // Ajouter nous-mêmes
        listData += (listData.isBlank() ? "" : ";") + username + "@" + localHost + ":" + localPort;

        Message listMsg = new Message();
        listMsg.setType(Message.Type.USER_LIST);
        listMsg.setSenderUsername(username);
        listMsg.setSenderHost(localHost);
        listMsg.setSenderPort(localPort);
        listMsg.setTextContent(listData);

        try {
            conn.send(listMsg);
        } catch (IOException e) {
            Logger.error("Impossible d'envoyer la liste à " + targetUsername);
        }
    }

    /**
     * Message texte reçu.
     */
    private void handleTextMessage(Message msg) {
        List<String> targets = msg.getTargetUsernames();
        // Si on est dans la liste des destinataires ou si c'est un broadcast (null)
        if (targets == null || targets.contains(username)) {
            listeners.forEach(l -> l.onMessageReceived(msg));
        }
    }

    /**
     * Fichier reçu.
     */
    private void handleFileMessage(Message msg) {
        List<String> targets = msg.getTargetUsernames();
        if (targets == null || targets.contains(username)) {
            listeners.forEach(l -> l.onFileReceived(msg));
        }
    }

    /**
     * Connexion TCP fermée avec un pair.
     */
    private void handleConnectionClosed(PeerConnection conn) {
        String peerUsername = conn.getPeer().getUsername();
        connections.remove(peerUsername);

        if (!peerUsername.startsWith("__")) {
            Peer peer = registry.getPeer(peerUsername);
            if (peer != null) {
                registry.removePeer(peerUsername);
                listeners.forEach(l -> l.onPeerDisconnected(peer));
                Logger.info("Connexion perdue avec: " + peerUsername);
            }
        }
    }

    // =========================================================================
    // Connexion sortante vers un pair connu
    // =========================================================================

    private void connectOutbound(String peerUsername, String host, int port) throws IOException {
        if (connections.containsKey(peerUsername)) return;

        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 5000);

        Peer peer = new Peer(peerUsername, host, port);

        PeerConnection conn = new PeerConnection(socket, peer,
                this::handleIncomingMessage, this::handleConnectionClosed);

        // Annoncer notre présence
        Message joinMsg = Message.createUserNotification(
                Message.Type.USER_JOIN, username, localHost, localPort);
        conn.send(joinMsg);

        conn.startListening();
        connections.put(peerUsername, conn);

        Logger.info("Connecté (sortant) à " + peerUsername + " @ " + host + ":" + port);
    }

    // =========================================================================
    // Envoi de messages
    // =========================================================================

    /**
     * Envoie un message texte à un ou plusieurs pairs.
     * @param targets null pour broadcast, liste de usernames sinon
     * @param text    contenu du message
     */
    public void sendTextMessage(List<String> targets, String text) {
        Message msg = Message.createTextMessage(username, localHost, localPort, targets, text);

        if (targets == null) {
            broadcast(msg);
        } else {
            targets.forEach(t -> sendToPeer(t, msg));
        }
    }

    /**
     * Envoie un fichier à un ou plusieurs pairs.
     */
    public void sendFile(List<String> targets, String fileName, byte[] data) {
        Message msg = Message.createFileMessage(username, localHost, localPort, targets, fileName, data);

        if (targets == null) {
            broadcast(msg);
        } else {
            targets.forEach(t -> sendToPeer(t, msg));
        }
    }

    private void sendToPeer(String targetUsername, Message msg) {
        PeerConnection conn = connections.get(targetUsername);
        if (conn == null || !conn.isConnected()) {
            Logger.warn("Pas de connexion active avec " + targetUsername);
            return;
        }
        try {
            conn.send(msg);
        } catch (IOException e) {
            Logger.error("Erreur envoi à " + targetUsername + ": " + e.getMessage());
        }
    }

    private void broadcast(Message msg) {
        connections.values().stream()
                .filter(c -> !c.getPeer().getUsername().startsWith("__"))
                .forEach(conn -> {
                    try {
                        conn.send(msg);
                    } catch (IOException e) {
                        Logger.error("Erreur broadcast vers " + conn.getPeer().getUsername());
                    }
                });
    }

    // =========================================================================
    // Accesseurs
    // =========================================================================

    public void addListener(MessageListener listener) {
        listeners.add(listener);
    }

    public void removeListener(MessageListener listener) {
        listeners.remove(listener);
    }

    public PeerRegistry getRegistry() { return registry; }
    public String getUsername() { return username; }
    public String getLocalHost() { return localHost; }
    public int getLocalPort() { return localPort; }
    public boolean isRunning() { return running; }
}

package uppa.group2;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class PeerConnection implements Closeable {

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final Peer peer;
    private final Consumer<Message> messageHandler;
    private final Consumer<PeerConnection> onClose;
    private Thread listenerThread;
    private volatile boolean running = true;

    public PeerConnection(Socket socket, Peer peer,
                          Consumer<Message> messageHandler,
                          Consumer<PeerConnection> onClose) throws IOException {
        this.socket = socket;
        this.peer = peer;
        this.messageHandler = messageHandler;
        this.onClose = onClose;

        // IMPORTANT: ObjectOutputStream en premier pour éviter le deadlock
        this.out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.out.flush();
        this.in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
    }

    /**
     * Démarre le thread d'écoute des messages entrants.
     */
    public void startListening() {
        listenerThread = new Thread(this::listenLoop, "Listener-" + peer.getUsername());
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void listenLoop() {
        try {
            while (running) {
                Object obj = in.readObject();
                if (obj instanceof Message) {
                    messageHandler.accept((Message) obj);
                }
            }
        } catch (EOFException | java.net.SocketException e) {
            // Connexion fermée normalement
        } catch (Exception e) {
            if (running) {
                Logger.error("Erreur lecture depuis " + peer.getUsername() + ": " + e.getMessage());
            }
        } finally {
            running = false;
            onClose.accept(this);
        }
    }

    /**
     * Envoie un message au pair distant.
     */
    public synchronized void send(Message message) throws IOException {
        out.writeObject(message);
        out.flush();
        out.reset(); // Evite les problèmes de cache de sérialisation
    }

    public Peer getPeer() { return peer; }

    public boolean isConnected() {
        return running && !socket.isClosed();
    }

    @Override
    public void close() {
        running = false;
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    @Override
    public String toString() {
        return "PeerConnection{" + peer + "}";
    }
}
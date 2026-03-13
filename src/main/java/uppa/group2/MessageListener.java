package uppa.group2;

public interface MessageListener {
    void onMessageReceived(Message message);

    void onFileReceived(Message message);

    void onPeerConnected(Peer peer);

    void onPeerDisconnected(Peer peer);

    void onPeerListReceived(java.util.List<Peer> peers);
}

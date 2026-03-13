package uppa.group2.core;

import uppa.group2.model.Message;
import uppa.group2.model.Peer;

public interface MessageListener {
    void onMessageReceived(Message message);

    void onFileReceived(Message message);

    void onPeerConnected(Peer peer);

    void onPeerDisconnected(Peer peer);

    void onPeerListReceived(java.util.List<Peer> peers);
}

package uppa.group2.core;

import uppa.group2.model.Peer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeerRegistry {

    // username -> Peer
    private final Map<String, Peer> peers = new ConcurrentHashMap<>();

    public void addPeer(Peer peer) {
        peers.put(peer.getUsername(), peer);
    }

    public void removePeer(String username) {
        peers.remove(username);
    }

    public Peer getPeer(String username) {
        return peers.get(username);
    }

    public boolean hasPeer(String username) {
        return peers.containsKey(username);
    }

    public List<Peer> getAllPeers() {
        return new ArrayList<>(peers.values());
    }

    public int size() {
        return peers.size();
    }

    public void clear() {
        peers.clear();
    }
}

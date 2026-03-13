package uppa.group2;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Peer implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String host;
    private int port;
    private LocalDateTime connectedAt;
    private boolean online;

    public Peer(String username, String host, int port) {
        this.username = username;
        this.host = host;
        this.port = port;
        this.connectedAt = LocalDateTime.now();
        this.online = true;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public LocalDateTime getConnectedAt() { return connectedAt; }
    public void setConnectedAt(LocalDateTime connectedAt) { this.connectedAt = connectedAt; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public String getAddress() {
        return host + ":" + port;
    }

    @Override
    public String toString() {
        return username + " (" + host + ":" + port + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Peer)) return false;
        Peer peer = (Peer) o;
        return username.equals(peer.username);
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }
}

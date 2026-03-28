package uppa.group2.model;

import java.io.Serializable;

public class User implements Serializable {

    private String username;
    private String host;
    private int port;

    public User(String username, String host, int port) {
        this.username = username;
        this.host = host;
        this.port = port;
    }

    public String getUsername() { return username; }
    public String getHost() { return host; }
    public int getPort() { return port; }

    @Override
    public String toString() {
        return username + " (" + host + ":" + port + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return host.equals(user.host) && port == user.port;
    }

    @Override
    public int hashCode() {
        return 31 * host.hashCode() + port;
    }

}

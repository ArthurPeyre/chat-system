package uppa.group2.network;

import uppa.group2.controller.ChatController;
import uppa.group2.model.User;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DiscoveryBroadcaster extends Thread {

    private final User localUser;
    private final int discoveryPort;

    public DiscoveryBroadcaster(User localUser, int discoveryPort) {
        this.localUser = localUser;
        this.discoveryPort = discoveryPort;
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true);

            String message = "DISCOVER|" + localUser.getUsername() + "|" + localUser.getHost() + "|" + localUser.getPort();
            byte[] data = message.getBytes();

            DatagramPacket packet = new DatagramPacket(
                data, data.length,
                InetAddress.getByName("255.255.255.255"),
                discoveryPort
            );

            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

package uppa.group2.network;

import uppa.group2.controller.ChatController;
import uppa.group2.model.User;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class DiscoveryBroadcaster extends Thread {

    private final User localUser;
    private final int discoveryPort;

    public DiscoveryBroadcaster(User localUser, int discoveryPort) {
        this.localUser = localUser;
        this.discoveryPort = discoveryPort;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try (MulticastSocket socket = new MulticastSocket()) {
            String message = "DISCOVER|" + localUser.getUsername() + "|"
                    + localUser.getHost() + "|" + localUser.getPort();
            byte[] data = message.getBytes();

            DatagramPacket packet = new DatagramPacket(
                    data, data.length,
                    InetAddress.getByName("230.0.0.1"), // adresse multicast
                    discoveryPort
            );
            socket.send(packet);
            System.out.println(">>> Broadcasting DISCOVER : " + message);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Dans DiscoveryBroadcaster, récupère aussi l'IP locale associée au broadcast
    private InetAddress getBroadcastAddress() throws IOException {
        java.util.Enumeration<java.net.NetworkInterface> interfaces =
                java.net.NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {
            java.net.NetworkInterface ni = interfaces.nextElement();
            if (ni.isLoopback() || !ni.isUp()) continue;

            for (java.net.InterfaceAddress ia : ni.getInterfaceAddresses()) {
                InetAddress broadcast = ia.getBroadcast();
                if (broadcast != null) {
                    // Affiche aussi l'IP locale associée
                    System.out.println("=== IP locale associée : " + ia.getAddress());
                    return broadcast;
                }
            }
        }
        return InetAddress.getByName("255.255.255.255");
    }

}

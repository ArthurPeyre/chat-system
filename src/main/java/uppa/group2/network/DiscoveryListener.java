package uppa.group2.network;

import uppa.group2.controller.ChatController;
import uppa.group2.model.User;

import java.io.IOException;
import java.net.*;

public class DiscoveryListener extends Thread {

    private final int discoveryPort;
    private final ChatController controller;
    private DatagramSocket socket;

    public DiscoveryListener(int discoveryPort, ChatController controller) {
        this.discoveryPort = discoveryPort;
        this.controller = controller;
    }

    @Override
    public void run() {
        try {
            MulticastSocket socket = new MulticastSocket(discoveryPort);
            InetAddress group = InetAddress.getByName("230.0.0.1");
            socket.joinGroup(group);
            this.socket = socket; // garde la référence pour stopListener()

            byte[] buffer = new byte[1024];
            while (!socket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength());
                // System.out.println("<<< Paquet reçu : " + received);

                String[] parts = received.split("\\|");

                if (parts[0].equals("DISCOVER")) {
                    User requester = new User(parts[1], parts[2], Integer.parseInt(parts[3]));
                    if (requester.equals(controller.getLocalUser())) continue;

                    System.out.println("<<< DISCOVER reçu de : " + requester);
                    sendResponse(packet.getAddress(), requester.getPort());
                    controller.handleDiscovery(requester);

                } else if (parts[0].equals("HERE")) {
                    System.out.println("<<< HERE reçu de : " + parts[1] + ", en attente de son bootstrap...");
                }
            }
        } catch (IOException e) {
            if (socket != null && !socket.isClosed()) {
                e.printStackTrace();
            }
        }
    }

    private void sendResponse(InetAddress address, int tcpPort) throws IOException {
        User local = controller.getLocalUser();
        String response = "HERE|" + local.getUsername() + "|" + local.getHost() + "|" + local.getPort();
        byte[] data = response.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, address, discoveryPort);
        socket.send(packet);
    }

    public void stopListner() {
        socket.close();
    }

}

package uppa.group2.network;

import uppa.group2.controller.ChatController;
import uppa.group2.model.User;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

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
            socket = new DatagramSocket(discoveryPort);
            byte[] buffer = new byte[1024];

            while (!socket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength());
                String[] parts = received.split("\\|");

                if (parts[0].equals("HERE")) {
                    User responder = new User(parts[1], parts[2], Integer.parseInt(parts[3]));
                    controller.handleDiscovery(responder);
                }
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                e.printStackTrace();
            }
        }
    }

    private void sendResponse(InetAddress address, int port) throws IOException {
        User local = controller.getLocalUser();
        String response = "HERE|" + local.getUsername() + "|" + local.getHost() + "|" + local.getPort();
        byte[] data = response.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        socket.send(packet);
    }

    public void stopListner() {
        socket.close();
    }

}

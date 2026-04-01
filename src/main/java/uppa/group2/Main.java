package uppa.group2;

import uppa.group2.controller.ChatController;
import uppa.group2.model.User;
import uppa.group2.view.ChatView;

import javax.swing.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                String username = promptUsername();
                int port = promptPort();
                String host = getLocalAddress();

                User localUser = new User(username, host, port);

                ChatView view = new ChatView();
                ChatController controller = new ChatController(localUser, view);
                view.setController(controller);

                view.setTitle("P2P ChatSystem — " + username);
                view.setVisible(true);

                controller.start();

            } catch (UnknownHostException e) {
                JOptionPane.showMessageDialog(null,
                        "Unable to determine local address : " + e.getMessage(),
                        "Network error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static String promptUsername() {
        String username = null;
        while (username == null || username.trim().isEmpty()) {
            username = JOptionPane.showInputDialog(null,
                    "Enter your username :", "Connection", JOptionPane.QUESTION_MESSAGE);
            if (username == null) System.exit(0); // Annulation
        }
        return username.trim();
    }

    private static int promptPort() {
        while (true) {
            String input = JOptionPane.showInputDialog(null,
                    "Enter your listening port:", "Connection", JOptionPane.QUESTION_MESSAGE);
            if (input == null) System.exit(0); // Annulation
            try {
                int port = Integer.parseInt(input.trim());
                if (port >= 1024 && port <= 65535) return port;
                JOptionPane.showMessageDialog(null,
                        "The port must be between 1024 and 65535.",
                        "Invalid port", JOptionPane.WARNING_MESSAGE);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null,
                        "Please enter a valid number.",
                        "Invalid port", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private static String getLocalAddress() throws IOException {
        java.util.Enumeration<java.net.NetworkInterface> interfaces =
                java.net.NetworkInterface.getNetworkInterfaces();

        while (interfaces.hasMoreElements()) {
            java.net.NetworkInterface ni = interfaces.nextElement();
            if (ni.isLoopback() || !ni.isUp()) continue;

            for (java.net.InterfaceAddress ia : ni.getInterfaceAddresses()) {
                if (ia.getBroadcast() != null &&
                        ia.getAddress() instanceof java.net.Inet4Address) {
                    return ia.getAddress().getHostAddress();
                }
            }
        }
        return InetAddress.getLocalHost().getHostAddress();
    }
}
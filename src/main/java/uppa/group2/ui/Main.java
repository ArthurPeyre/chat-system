package uppa.group2.ui;

import uppa.group2.Logger;
import uppa.group2.P2PNode;

import javax.swing.*;

public class Main {

    public static void main(String[] args) {
        // Configurer le look and feel avant tout
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Activer le debug si demandé
        if (args.length > 0 && args[0].equals("--debug")) {
            Logger.setDebug(true);
            Logger.info("Mode debug activé");
        }

        // Afficher le dialogue de connexion (sur le thread Swing)
        SwingUtilities.invokeLater(() -> {
            LoginDialog loginDialog = new LoginDialog();
            loginDialog.setVisible(true);

            if (!loginDialog.isConfirmed()) {
                System.exit(0);
                return;
            }

            String username = loginDialog.getUsername();
            int port = loginDialog.getPort();

            Logger.info("Démarrage avec pseudonyme=" + username + ", port=" + port);

            try {
                // Créer et démarrer le nœud P2P
                P2PNode node = new P2PNode(username, port);
                node.start();

                // Afficher la fenêtre principale
                ChatWindow window = new ChatWindow(node);
                window.setVisible(true);

            } catch (java.net.BindException e) {
                JOptionPane.showMessageDialog(null,
                        "Le port " + port + " est déjà utilisé.\n" +
                                "Veuillez choisir un autre port.",
                        "Port indisponible", JOptionPane.ERROR_MESSAGE);
                Logger.error("Port déjà utilisé: " + port);
                System.exit(1);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Erreur au démarrage: " + e.getMessage(),
                        "Erreur", JOptionPane.ERROR_MESSAGE);
                Logger.error("Erreur démarrage: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        });
    }
}
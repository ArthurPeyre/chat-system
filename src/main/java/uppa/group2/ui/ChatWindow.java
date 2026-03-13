package uppa.group2.ui;

import uppa.group2.core.MessageListener;
import uppa.group2.model.Message;
import uppa.group2.model.Peer;
import uppa.group2.network.P2PNode;
import uppa.group2.util.FileUtils;
import uppa.group2.util.NetworkUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class ChatWindow extends JFrame implements MessageListener {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Color COLOR_BG = new Color(30, 30, 46);
    private static final Color COLOR_PANEL = new Color(49, 50, 68);
    private static final Color COLOR_ACCENT = new Color(137, 180, 250);
    private static final Color COLOR_TEXT = new Color(205, 214, 244);
    private static final Color COLOR_MUTED = new Color(108, 112, 134);
    private static final Color COLOR_MSG_SELF = new Color(166, 227, 161);
    private static final Color COLOR_MSG_OTHER = new Color(243, 139, 168);
    private static final Color COLOR_MSG_SYSTEM = new Color(249, 226, 175);
    private static final Color COLOR_ONLINE = new Color(166, 227, 161);
    private static final Color COLOR_SELECTED = new Color(69, 71, 90);

    private final P2PNode node;

    // Composants UI
    private JTextArea chatArea;
    private JTextField inputField;
    private DefaultListModel<Peer> peerListModel;
    private JList<Peer> peerList;
    private JLabel statusLabel;
    private JLabel connectedLabel;
    private JButton sendButton;
    private JButton sendFileButton;
    private JButton connectButton;

    public ChatWindow(P2PNode node) {
        this.node = node;
        node.addListener(this);

        setTitle("P2P Chat — " + node.getUsername());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(900, 650);
        setMinimumSize(new Dimension(700, 500));
        setLocationRelativeTo(null);

        initUI();
        setupCloseHandler();

        // Afficher notre adresse dans le statut
        updateStatus();
        appendSystemMessage("Bienvenue, " + node.getUsername() + "! " +
                "Votre adresse: " + node.getLocalHost() + ":" + node.getLocalPort());
        appendSystemMessage("Utilisez 'Connecter un pair' pour rejoindre le réseau.");
    }

    // =========================================================================
    // Construction de l'interface
    // =========================================================================

    private void initUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        getContentPane().setBackground(COLOR_BG);
        setLayout(new BorderLayout());

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildMainPanel(), BorderLayout.CENTER);
        add(buildInputPanel(), BorderLayout.SOUTH);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(COLOR_PANEL);
        bar.setBorder(new EmptyBorder(8, 12, 8, 12));

        // Icône + nom
        JLabel nameLabel = new JLabel("💬 P2P Chat — " + node.getUsername());
        nameLabel.setForeground(COLOR_ACCENT);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 15f));

        // Adresse locale
        statusLabel = new JLabel(node.getLocalHost() + ":" + node.getLocalPort());
        statusLabel.setForeground(COLOR_MUTED);
        statusLabel.setFont(statusLabel.getFont().deriveFont(12f));

        // Pairs connectés
        connectedLabel = new JLabel("0 pair(s) connecté(s)");
        connectedLabel.setForeground(COLOR_ONLINE);
        connectedLabel.setFont(connectedLabel.getFont().deriveFont(12f));

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        connectButton = createStyledButton("🔗 Connecter un pair", COLOR_ACCENT);
        connectButton.addActionListener(e -> showConnectDialog());

        rightPanel.add(statusLabel);
        rightPanel.add(Box.createHorizontalStrut(10));
        rightPanel.add(connectedLabel);
        rightPanel.add(Box.createHorizontalStrut(10));
        rightPanel.add(connectButton);

        bar.add(nameLabel, BorderLayout.WEST);
        bar.add(rightPanel, BorderLayout.EAST);

        return bar;
    }

    private JSplitPane buildMainPanel() {
        // === Panneau pairs (gauche) ===
        peerListModel = new DefaultListModel<>();
        peerList = new JList<>(peerListModel);
        peerList.setBackground(COLOR_PANEL);
        peerList.setForeground(COLOR_TEXT);
        peerList.setSelectionBackground(COLOR_SELECTED);
        peerList.setSelectionForeground(COLOR_ACCENT);
        peerList.setBorder(new EmptyBorder(4, 4, 4, 4));
        peerList.setCellRenderer(new PeerCellRenderer());
        peerList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        peerList.setToolTipText("Sélectionnez un ou plusieurs pairs pour envoyer un message privé. Aucune sélection = broadcast.");

        JScrollPane peerScroll = new JScrollPane(peerList);
        peerScroll.setBorder(BorderFactory.createEmptyBorder());
        peerScroll.getViewport().setBackground(COLOR_PANEL);

        JLabel peerHeaderLabel = new JLabel("  👥 Pairs connectés");
        peerHeaderLabel.setForeground(COLOR_ACCENT);
        peerHeaderLabel.setFont(peerHeaderLabel.getFont().deriveFont(Font.BOLD, 12f));
        peerHeaderLabel.setBorder(new EmptyBorder(6, 4, 6, 4));
        peerHeaderLabel.setOpaque(true);
        peerHeaderLabel.setBackground(COLOR_PANEL);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(COLOR_PANEL);
        leftPanel.add(peerHeaderLabel, BorderLayout.NORTH);
        leftPanel.add(peerScroll, BorderLayout.CENTER);

        // Bouton désélectionner
        JButton clearSelBtn = createStyledButton("🌐 Broadcast", COLOR_MUTED);
        clearSelBtn.setToolTipText("Désélectionner pour envoyer à tous");
        clearSelBtn.addActionListener(e -> peerList.clearSelection());
        clearSelBtn.setFont(clearSelBtn.getFont().deriveFont(10f));
        leftPanel.add(clearSelBtn, BorderLayout.SOUTH);

        // === Zone de chat (droite) ===
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(COLOR_BG);
        chatArea.setForeground(COLOR_TEXT);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createEmptyBorder());
        chatScroll.getViewport().setBackground(COLOR_BG);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, chatScroll);
        split.setDividerLocation(220);
        split.setDividerSize(4);
        split.setBackground(COLOR_BG);
        split.setBorder(null);

        return split;
    }

    private JPanel buildInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBackground(COLOR_PANEL);
        panel.setBorder(new EmptyBorder(8, 12, 8, 12));

        inputField = new JTextField();
        inputField.setBackground(new Color(69, 71, 90));
        inputField.setForeground(COLOR_TEXT);
        inputField.setCaretColor(COLOR_TEXT);
        inputField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_ACCENT, 1),
                new EmptyBorder(6, 8, 6, 8)));
        inputField.addActionListener(e -> sendMessage());
        inputField.setToolTipText("Tapez votre message et appuyez sur Entrée");

        sendButton = createStyledButton("Envoyer ➤", COLOR_ACCENT);
        sendButton.addActionListener(e -> sendMessage());

        sendFileButton = createStyledButton("📎 Fichier", new Color(203, 166, 247));
        sendFileButton.addActionListener(e -> sendFile());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(sendFileButton);
        btnPanel.add(sendButton);

        panel.add(inputField, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.EAST);

        return panel;
    }

    private JButton createStyledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setForeground(color);
        btn.setBackground(COLOR_PANEL);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, 1),
                new EmptyBorder(5, 10, 5, 10)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFont(btn.getFont().deriveFont(Font.PLAIN, 12f));

        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBackground(color);
                btn.setForeground(COLOR_BG);
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(COLOR_PANEL);
                btn.setForeground(color);
            }
        });

        return btn;
    }

    // =========================================================================
    // Dialogues
    // =========================================================================

    private void showConnectDialog() {
        JTextField hostField = new JTextField("127.0.0.1", 15);
        JTextField portField = new JTextField("5000", 6);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(COLOR_PANEL);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);

        addFormRow(panel, gbc, 0, "Hôte distant:", hostField);
        addFormRow(panel, gbc, 1, "Port:", portField);

        JLabel infoLabel = new JLabel("<html><i>Ex: 192.168.1.10:5000</i></html>");
        infoLabel.setForeground(COLOR_MUTED);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        panel.add(infoLabel, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Connexion à un pair", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String host = hostField.getText().trim();
            String portStr = portField.getText().trim();
            try {
                int port = Integer.parseInt(portStr);
                connectToPeerAsync(host, port);
            } catch (NumberFormatException e) {
                showError("Port invalide: " + portStr);
            }
        }
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row,
                            String label, JComponent field) {
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.WEST;
        JLabel lbl = new JLabel(label);
        lbl.setForeground(COLOR_TEXT);
        panel.add(lbl, gbc);

        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        field.setBackground(new Color(69, 71, 90));
        if (field instanceof JTextField) ((JTextField) field).setForeground(COLOR_TEXT);
        panel.add(field, gbc);
    }

    private void connectToPeerAsync(String host, int port) {
        appendSystemMessage("Connexion en cours vers " + host + ":" + port + "...");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                node.connectToPeer(host, port);
                return null;
            }
            @Override
            protected void done() {
                try {
                    get();
                    appendSystemMessage("✅ Connecté à " + host + ":" + port);
                } catch (Exception e) {
                    showError("Connexion échouée: " + e.getCause().getMessage());
                    appendSystemMessage("❌ Connexion échouée vers " + host + ":" + port);
                }
            }
        };
        worker.execute();
    }

    // =========================================================================
    // Envoi de messages
    // =========================================================================

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        List<String> targets = getSelectedPeerUsernames();
        String targetStr = targets == null ? "Tous" : String.join(", ", targets);

        // Afficher dans notre propre chat
        appendChatMessage("[Vous → " + targetStr + "]", text, COLOR_MSG_SELF);

        node.sendTextMessage(targets, text);
        inputField.setText("");
        inputField.requestFocus();
    }

    private void sendFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Sélectionner un fichier à envoyer");
        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            List<String> targets = getSelectedPeerUsernames();
            String targetStr = targets == null ? "Tous" : String.join(", ", targets);

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    byte[] data = FileUtils.readFile(file);
                    node.sendFile(targets, file.getName(), data);
                    return null;
                }
                @Override
                protected void done() {
                    try {
                        get();
                        appendChatMessage("[Fichier envoyé → " + targetStr + "]",
                                "📎 " + file.getName() +
                                        " (" + NetworkUtils.formatFileSize(file.length()) + ")",
                                COLOR_MSG_SELF);
                    } catch (Exception e) {
                        showError("Erreur envoi fichier: " + e.getCause().getMessage());
                    }
                }
            };
            worker.execute();
        }
    }

    /**
     * Retourne les usernames sélectionnés dans la liste (null si aucun = broadcast).
     */
    private List<String> getSelectedPeerUsernames() {
        List<Peer> selected = peerList.getSelectedValuesList();
        if (selected.isEmpty()) return null;
        return selected.stream().map(Peer::getUsername).collect(Collectors.toList());
    }

    // =========================================================================
    // Implémentation MessageListener (appelé depuis threads réseau)
    // =========================================================================

    @Override
    public void onMessageReceived(Message message) {
        SwingUtilities.invokeLater(() -> {
            String from = message.getSenderUsername();
            List<String> targets = message.getTargetUsernames();
            String dest = (targets == null) ? "Broadcast" : "Vous";
            appendChatMessage("[" + from + " → " + dest + "]",
                    message.getTextContent(), COLOR_MSG_OTHER);
        });
    }

    @Override
    public void onFileReceived(Message message) {
        SwingUtilities.invokeLater(() -> {
            String from = message.getSenderUsername();
            String fileName = message.getFileName();
            long size = message.getFileSize();

            appendChatMessage("[Fichier reçu de " + from + "]",
                    "📎 " + fileName + " (" + NetworkUtils.formatFileSize(size) + ")",
                    new Color(203, 166, 247));

            // Proposer de sauvegarder
            int choice = JOptionPane.showConfirmDialog(this,
                    "Fichier reçu de " + from + ":\n" + fileName +
                            " (" + NetworkUtils.formatFileSize(size) + ")\n\nSauvegarder?",
                    "Fichier reçu", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);

            if (choice == JOptionPane.YES_OPTION) {
                JFileChooser chooser = new JFileChooser();
                chooser.setSelectedFile(new File(fileName));
                chooser.setDialogTitle("Sauvegarder le fichier");
                int res = chooser.showSaveDialog(this);

                if (res == JFileChooser.APPROVE_OPTION) {
                    File dest = chooser.getSelectedFile();
                    try {
                        FileUtils.saveFile(dest.getParent(), dest.getName(),
                                message.getFileData());
                        appendSystemMessage("✅ Fichier sauvegardé: " + dest.getAbsolutePath());
                    } catch (IOException e) {
                        showError("Erreur sauvegarde: " + e.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public void onPeerConnected(Peer peer) {
        SwingUtilities.invokeLater(() -> {
            // Eviter les doublons dans la liste
            for (int i = 0; i < peerListModel.size(); i++) {
                if (peerListModel.get(i).getUsername().equals(peer.getUsername())) return;
            }
            peerListModel.addElement(peer);
            updateConnectedCount();
            appendSystemMessage("🟢 " + peer.getUsername() + " a rejoint le réseau (" +
                    peer.getHost() + ":" + peer.getPort() + ")");
        });
    }

    @Override
    public void onPeerDisconnected(Peer peer) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < peerListModel.size(); i++) {
                if (peerListModel.get(i).getUsername().equals(peer.getUsername())) {
                    peerListModel.remove(i);
                    break;
                }
            }
            updateConnectedCount();
            appendSystemMessage("🔴 " + peer.getUsername() + " a quitté le réseau.");
        });
    }

    @Override
    public void onPeerListReceived(List<Peer> peers) {
        SwingUtilities.invokeLater(() -> {
            for (Peer peer : peers) {
                boolean exists = false;
                for (int i = 0; i < peerListModel.size(); i++) {
                    if (peerListModel.get(i).getUsername().equals(peer.getUsername())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    peerListModel.addElement(peer);
                }
            }
            updateConnectedCount();
            if (!peers.isEmpty()) {
                appendSystemMessage("📋 Pairs existants trouvés: " +
                        peers.stream().map(Peer::getUsername).collect(Collectors.joining(", ")));
            }
        });
    }

    // =========================================================================
    // Utilitaires UI
    // =========================================================================

    private void appendChatMessage(String header, String body, Color headerColor) {
        SwingUtilities.invokeLater(() -> {
            javax.swing.text.Document doc = chatArea.getDocument();
            try {
                // On utilise un TextArea simple — le header est sur une ligne, le message sur la suivante
                String timestamp = java.time.LocalTime.now().format(TIME_FMT);
                chatArea.append(timestamp + " " + header + "\n");
                chatArea.append("  " + body + "\n\n");
                chatArea.setCaretPosition(doc.getLength());
            } catch (Exception ignored) {}
        });
    }

    private void appendSystemMessage(String text) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = java.time.LocalTime.now().format(TIME_FMT);
            chatArea.append(timestamp + " ── " + text + " ──\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void updateConnectedCount() {
        int count = peerListModel.size();
        connectedLabel.setText(count + " pair" + (count > 1 ? "s" : "") + " connecté" + (count > 1 ? "s" : ""));
    }

    private void updateStatus() {
        statusLabel.setText(node.getLocalHost() + ":" + node.getLocalPort());
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Erreur", JOptionPane.ERROR_MESSAGE);
    }

    private void setupCloseHandler() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int choice = JOptionPane.showConfirmDialog(ChatWindow.this,
                        "Voulez-vous quitter P2P Chat?", "Quitter",
                        JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    node.stop();
                    dispose();
                    System.exit(0);
                }
            }
        });
    }

    // =========================================================================
    // Renderer de la liste des pairs
    // =========================================================================

    private static class PeerCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof Peer peer) {
                setText("<html><b>" + peer.getUsername() + "</b><br>" +
                        "<small><font color='#6c7086'>" + peer.getHost() + ":" + peer.getPort() + "</font></small></html>");
                setIcon(null);
                setBorder(new EmptyBorder(4, 8, 4, 8));
                setForeground(isSelected ? COLOR_ACCENT : COLOR_TEXT);

                // Point vert devant le nom
                setIcon(new Icon() {
                    public void paintIcon(Component c, Graphics g, int x, int y) {
                        g.setColor(COLOR_ONLINE);
                        g.fillOval(x, y + 4, 8, 8);
                    }
                    public int getIconWidth() { return 12; }
                    public int getIconHeight() { return 16; }
                });
            }
            return this;
        }
    }

    // Constantes de couleur (répétées ici pour le renderer static)
    /*
    private static final Color COLOR_ACCENT = new Color(137, 180, 250);
    private static final Color COLOR_TEXT = new Color(205, 214, 244);
    private static final Color COLOR_ONLINE = new Color(166, 227, 161);
    private static final Color COLOR_MUTED = new Color(108, 112, 134);
    */
}

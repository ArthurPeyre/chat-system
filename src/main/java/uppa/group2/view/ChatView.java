package uppa.group2.view;

import uppa.group2.controller.ChatController;
import uppa.group2.model.Message;
import uppa.group2.model.User;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatView extends JFrame {

    private ChatController controller;

    // Composants principaux
    private final JList<User> userList;
    private final DefaultListModel<User> userListModel;
    private final JTextArea messageArea;
    private final JTextField messageField;
    private final JButton sendButton;

    public ChatView() {
        super("P2P Chat");

        // Connected Users List
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        userList.setBorder(BorderFactory.createTitledBorder("Connected Users"));
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(200, 0));

        // Messages area
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane messageScroll = new JScrollPane(messageArea);

        // Inputs & buttons area
        messageField = new JTextField();
        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> handleSend());
        messageField.addActionListener(e -> handleSend()); // Envoi avec Entrée

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        JButton fileButton = new JButton("📎 Files");
        fileButton.addActionListener(e -> handleSendFile());

        inputPanel.add(fileButton, BorderLayout.WEST);

        // General layout
        setLayout(new BorderLayout(2, 2));
        add(userScroll, BorderLayout.WEST);
        add(messageScroll, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        // Window
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                controller.disconnect();
                dispose();
                System.exit(0);
            }
        });
    }

    public void setController(ChatController controller) {
        this.controller = controller;
    }






    private void handleSend() {
        String content = messageField.getText().trim();
        List<User> selected = userList.getSelectedValuesList();

        if (content.isEmpty()) {
            showError("Message can't be empty.");
            return;
        }

        List<User> recipients = selected.isEmpty()
                ? new ArrayList<>(controller.getConnectedUsers()) // broadcast
                : selected;

        controller.sendMessage(content, recipients);
        Message message = new Message(controller.getLocalUser(), controller.getConnectedUsers(), content, Message.Type.TEXT);
        showMessage(message);
        messageField.setText("");
    }

    // Nouvelle méthode handleSendFile
    private void handleSendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = fileChooser.getSelectedFile();
        List<User> selected = userList.getSelectedValuesList();
        List<User> recipients = selected.isEmpty()
                ? new ArrayList<>(controller.getConnectedUsers())
                : selected;

        if (recipients.isEmpty()) {
            showError("No users are currently connected.");
            return;
        }

        controller.sendFile(file, recipients);
    }




    public void showConnectedUsers(List<User> users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            users.forEach(userListModel::addElement);
        });
    }

    public void showMessage(Message message) {
        String sender;
        if (controller.getLocalUser().equals(message.getSender())) {
            sender = "You";
        } else {
            sender = message.getSender().getUsername();
        }
        SwingUtilities.invokeLater(() -> {
            messageArea.append("[" + message.getTimestamp().toLocalTime().withNano(0)
                    + "] " + sender
                    + " : " + message.getContent() + "\n");
        });
    }

    // Méthode appelée par le controller à la réception d'un fichier
    public void showReceivedFile(Message message) {
        SwingUtilities.invokeLater(() -> {
            String fileName = message.getContent();
            byte[] fileData = message.getFileData();
            String sender = message.getSender().getUsername();

            messageArea.append("📎 " + sender + " sent you : " + fileName + "\n");

            // Affichage si image
            if (isImage(fileName)) {
                ImageIcon icon = new ImageIcon(fileData);
                Image scaled = icon.getImage().getScaledInstance(200, -1, Image.SCALE_SMOOTH);
                JLabel imgLabel = new JLabel(new ImageIcon(scaled));
                JOptionPane.showMessageDialog(this, imgLabel, "Image from " + sender, JOptionPane.PLAIN_MESSAGE);
            }
            // Affichage si texte
            else if (isText(fileName)) {
                String text = new String(fileData);
                JTextArea textArea = new JTextArea(text);
                textArea.setEditable(false);
                JScrollPane scroll = new JScrollPane(textArea);
                scroll.setPreferredSize(new Dimension(400, 300));
                JOptionPane.showMessageDialog(this, scroll, "Text file from " + sender, JOptionPane.PLAIN_MESSAGE);
            }

            // Proposer la sauvegarde dans tous les cas
            int choice = JOptionPane.showConfirmDialog(this,
                    "Save \"" + fileName + "\" ?",
                    "File received", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                JFileChooser chooser = new JFileChooser();
                chooser.setSelectedFile(new File(fileName));
                if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    try (FileOutputStream fos = new FileOutputStream(chooser.getSelectedFile())) {
                        fos.write(fileData);
                        messageArea.append("File saved.\n");
                    } catch (IOException e) {
                        showError("Error during backup : " + e.getMessage());
                    }
                }
            }
        });
    }

    private boolean isImage(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg") || lower.endsWith(".gif");
    }

    private boolean isText(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".txt") || lower.endsWith(".csv")
                || lower.endsWith(".json") || lower.endsWith(".xml");
    }

    public void showUserConnected(User user) {
        SwingUtilities.invokeLater(() ->
                messageArea.append("*** " + user.getUsername() + " joined the chat ***\n")
        );
    }

    public void showUserDisconnected(User user) {
        SwingUtilities.invokeLater(() ->
                messageArea.append("*** " + user.getUsername() + " left the chat ***\n")
        );
    }

    public void showError(String message) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

}

package uppa.group2.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        TEXT,           // Message texte normal
        FILE,           // Transfert de fichier
        USER_JOIN,      // Notification connexion
        USER_LEAVE,     // Notification déconnexion
        USER_LIST,      // Liste des utilisateurs connectés
        USER_LIST_REQUEST, // Demande de liste
        PING,           // Maintien connexion
        ACK             // Accusé de réception
    }

    private Type type;
    private String senderUsername;
    private String senderHost;
    private int senderPort;
    private List<String> targetUsernames; // null = broadcast
    private String textContent;
    private String fileName;
    private byte[] fileData;
    private long fileSize;
    private LocalDateTime timestamp;
    private String messageId;

    public Message() {
        this.timestamp = LocalDateTime.now();
        this.messageId = java.util.UUID.randomUUID().toString();
    }

    // Constructeur message texte
    public static Message createTextMessage(String sender, String host, int port,
                                            List<String> targets, String text) {
        Message m = new Message();
        m.type = Type.TEXT;
        m.senderUsername = sender;
        m.senderHost = host;
        m.senderPort = port;
        m.targetUsernames = targets;
        m.textContent = text;
        return m;
    }

    // Constructeur transfert de fichier
    public static Message createFileMessage(String sender, String host, int port,
                                            List<String> targets, String fileName,
                                            byte[] fileData) {
        Message m = new Message();
        m.type = Type.FILE;
        m.senderUsername = sender;
        m.senderHost = host;
        m.senderPort = port;
        m.targetUsernames = targets;
        m.fileName = fileName;
        m.fileData = fileData;
        m.fileSize = fileData != null ? fileData.length : 0;
        return m;
    }

    // Constructeur notification connexion/déconnexion
    public static Message createUserNotification(Type type, String username,
                                                 String host, int port) {
        Message m = new Message();
        m.type = type;
        m.senderUsername = username;
        m.senderHost = host;
        m.senderPort = port;
        return m;
    }

    // Getters & Setters
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }

    public String getSenderHost() { return senderHost; }
    public void setSenderHost(String senderHost) { this.senderHost = senderHost; }

    public int getSenderPort() { return senderPort; }
    public void setSenderPort(int senderPort) { this.senderPort = senderPort; }

    public List<String> getTargetUsernames() { return targetUsernames; }
    public void setTargetUsernames(List<String> targetUsernames) { this.targetUsernames = targetUsernames; }

    public String getTextContent() { return textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
        this.fileSize = fileData != null ? fileData.length : 0;
    }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    @Override
    public String toString() {
        return String.format("Message{type=%s, from=%s@%s:%d, targets=%s}",
                type, senderUsername, senderHost, senderPort, targetUsernames);
    }
}

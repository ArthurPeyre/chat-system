package uppa.group2.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class Message implements Serializable {

    public enum Type { TEXT, FILE, CONNECT, DISCONNECT }

    private User sender;
    private List<User> recipients;
    private String content;
    private byte[] fileData;
    private LocalDateTime timestamp;
    private Type type;

    public Message(User sender, List<User> recipients, String content, Type type) {
        this.sender = sender;
        this.recipients = recipients;
        this.content = content;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }

    // Constructeur fichier
    public Message(User sender, List<User> recipients, String fileName, byte[] fileData) {
        this.sender = sender;
        this.recipients = recipients;
        this.content = fileName;
        this.fileData = fileData;
        this.type = Type.FILE;
        this.timestamp = LocalDateTime.now();
    }

    public User getSender() { return sender; }
    public List<User> getRecipients() { return recipients; }
    public String getContent() { return content; }
    public byte[] getFileData() { return fileData; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Type getType() { return type; }

    @Override
    public String toString() {
        return "[" + timestamp + "] " + sender.getUsername() + " -> " + content;
    }

}

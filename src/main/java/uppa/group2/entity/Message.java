package uppa.group2.entity;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class Message {

    private UUID id;
    private String content;
    private User sender;
    private ChatRoom room;
    private LocalDateTime timestamp;
    private TrayIcon.MessageType type; // TEXT, JOIN, LEAVE

    public Message(UUID uuid, String content, User user, LocalDateTime now) {
        this.id = uuid;
        this.content = content;
        this.sender = user;
        this.timestamp = now;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public ChatRoom getRoom() {
        return room;
    }

    public void setRoom(ChatRoom room) {
        this.room = room;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public TrayIcon.MessageType getType() {
        return type;
    }

    public void setType(TrayIcon.MessageType type) {
        this.type = type;
    }
}

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

}

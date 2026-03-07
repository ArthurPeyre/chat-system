package uppa.group2.viewmodel;

import com.google.gson.Gson;
import uppa.group2.entity.Message;

import java.time.format.DateTimeFormatter;

public class MessageViewModel {

    private String senderUsername;
    private String content;
    private String formattedTime;
    private String type;

    private MessageViewModel(String senderUsername, String content, String formattedTime, String type) {
        this.senderUsername = senderUsername;
        this.content = content;
        this.formattedTime = formattedTime;
        this.type = type;
    }

    public static MessageViewModel from(Message message) {
        String time = message.getTimestamp()
                .format(DateTimeFormatter.ofPattern("HH:mm"));

        return new MessageViewModel(
                message.getSender().getUsername(),
                message.getContent(),
                time,
                message.getType().name()
        );
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static MessageViewModel fromJson(String json) {
        return new Gson().fromJson(json, MessageViewModel.class);
    }

    @Override
    public String toString() {
        return "[" + formattedTime + "] " + senderUsername + " : " + content;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public String getContent() {
        return content;
    }

    public String getFormattedTime() {
        return formattedTime;
    }

    public String getType() {
        return type;
    }
}

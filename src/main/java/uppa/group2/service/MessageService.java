package uppa.group2.service;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import uppa.group2.entity.Message;
import uppa.group2.viewmodel.MessageViewModel;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class MessageService {

    private final List<Message> history = new ArrayList<>();
    private static final String FILE_PATH = "D:/Formations/L3_NEC/server-data/messages.json";
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>)
                    (src, typeOfSrc, context) -> new JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>)
                    (json, typeOfT, context) -> LocalDateTime.parse(json.getAsString()))
            .create();

    public MessageService() {
        loadFromDisk();
    }

    public void save(Message message) {
        history.add(message);
        if (persistToDisk()) IO.println("Nouveau message ! Émetteur: " + message.getSender().getUsername());
    }

    public List<Message> getAll() {
        return Collections.unmodifiableList(history);
    }

    public List<MessageViewModel> getRecent(int limit) {
        return history.stream()
                .sorted(Comparator.comparing(Message::getTimestamp).reversed())
                .limit(limit)
                .map(MessageViewModel::from)
                .collect(Collectors.toList());
    }

    public List<MessageViewModel> getByUser(String username) {
        return history.stream()
                .filter(msg -> msg.getSender().getUsername().equals(username))
                .map(MessageViewModel::from)
                .collect(Collectors.toList());
    }

    public List<MessageViewModel> search(String keyword) {
        return history.stream()
                .filter(msg -> msg.getContent().toLowerCase().contains(keyword.toLowerCase()))
                .map(MessageViewModel::from)
                .collect(Collectors.toList());
    }

    public Map<String, Long> countByUser() {
        return history.stream()
                .collect(Collectors.groupingBy(msg -> msg.getSender().getUsername(), Collectors.counting()));
    }

    private Boolean persistToDisk() {
        try {
            Path filePath = Path.of(FILE_PATH);
            Path parentDir = filePath.getParent();

            Files.createDirectories(parentDir);
            String json = gson.toJson(history);
            Files.writeString(filePath, json);

            return true;
        } catch (IOException e) {
            IO.println("Erreur écriture: " + e.getMessage());
        }

        return false;
    }

    private void loadFromDisk() {
        try {
            if (!Files.exists(Path.of(FILE_PATH))) return;
            String json = Files.readString(Path.of(FILE_PATH));
            Type listType = new TypeToken<List<Message>>(){}.getType();
            List<Message> loaded = gson.fromJson(json, listType);
            if (loaded != null) history.addAll(loaded);
            IO.println(history.size() + " messages chargés depuis le disque");
        } catch (IOException e) {
            IO.println("Erreur lecture: " + e.getMessage());
        }
    }

}

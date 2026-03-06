# Chat System

## Structure

```
    chat-system/
    ├── src/main/java/com/chat/
    │   ├── entity/
    │   │   ├── User.java
    │   │   ├── Message.java
    │   │   └── ChatRoom.java
    │   │
    │   ├── viewmodel/
    │   │   ├── MessageViewModel.java
    │   │   └── UserViewModel.java
    │   │
    │   ├── server/
    │   │   ├── ChatServer.java
    │   │   └── ClientHandler.java
    │   │
    │   ├── client/
    │   │   ├── ChatClient.java
    │   │   └── ConsoleUI.java
    │   │
    │   └── service/
    │       ├── MessageService.java
    │       └── UserService.java
```
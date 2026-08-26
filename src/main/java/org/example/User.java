package org.example;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class User implements Serializable {
    private static final long serialVersionUID = 1L; // חשוב לשמירת קבצים ב-Java

    private final long chatId;
    private final String firstName;
    private final String username;
    private final LocalDateTime joinDate;

    public User(long chatId, String firstName, String username) {
        this.chatId = chatId;
        this.firstName = firstName;
        this.username = (username != null && !username.isEmpty()) ? "@" + username : "-";
        this.joinDate = LocalDateTime.now();
    }

    public long getChatId() { return chatId; }
    public String getFirstName() { return firstName; }
    public String getUsername() { return username; }
    public LocalDateTime getJoinDate() { return joinDate; }

    public String getFormattedJoinTime() {
        return joinDate.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
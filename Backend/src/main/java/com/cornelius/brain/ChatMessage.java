package com.cornelius.brain;

public record ChatMessage(
        String role, // "user", "assistant", "system"
        String content,
        long timestamp
) {
    public static ChatMessage user(String content) {
        return new ChatMessage("user", content, System.currentTimeMillis());
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content, System.currentTimeMillis());
    }

    public static ChatMessage system(String content) {
        return new ChatMessage("system", content, System.currentTimeMillis());
    }
}


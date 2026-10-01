package com.learningdashboard.backend.generation.model;

/** One turn of a Spark chat: role is "user" or "assistant". The server never stores a chat - the app sends it each time. */
public record ChatMessage(String role, String text) {

    public static final String USER = "user";
    public static final String ASSISTANT = "assistant";

    public boolean fromUser() {
        return USER.equals(role);
    }
}

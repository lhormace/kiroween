package com.health.chat.service;

import com.health.chat.model.ChatResponse;

public interface ChatService {
    ChatResponse processMessage(String userId, String message, String token);
}

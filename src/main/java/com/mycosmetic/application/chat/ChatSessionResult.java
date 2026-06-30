package com.mycosmetic.application.chat;

import com.mycosmetic.domain.chat.ChatSession;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatSessionResult(
        UUID id,
        LocalDateTime createdAt
) {
    public static ChatSessionResult from(ChatSession session) {
        return new ChatSessionResult(session.getId(), session.getCreatedAt());
    }
}

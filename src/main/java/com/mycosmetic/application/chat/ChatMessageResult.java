package com.mycosmetic.application.chat;

import com.mycosmetic.domain.chat.ChatMessage;
import com.mycosmetic.domain.chat.Role;

import java.time.LocalDateTime;

public record ChatMessageResult(
        Long id,
        Role role,
        String content,
        LocalDateTime createdAt
) {
    public static ChatMessageResult from(ChatMessage message) {
        return new ChatMessageResult(
                message.getId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}

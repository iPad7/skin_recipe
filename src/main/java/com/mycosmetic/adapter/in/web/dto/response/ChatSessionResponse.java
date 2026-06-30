package com.mycosmetic.adapter.in.web.dto.response;

import com.mycosmetic.application.chat.ChatSessionResult;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class ChatSessionResponse {
    private final UUID id;
    private final LocalDateTime createdAt;

    private ChatSessionResponse(UUID id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public static ChatSessionResponse from(ChatSessionResult result) {
        return new ChatSessionResponse(result.id(), result.createdAt());
    }
}

package com.mycosmetic.adapter.in.web.dto.response;

import com.mycosmetic.domain.chat.ChatSession;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class ChatSessionResponse {
    private final UUID id;
    private final LocalDateTime createdAt;

    public ChatSessionResponse(ChatSession session) {
        this.id = session.getId();
        this.createdAt = session.getCreatedAt();
    }
}

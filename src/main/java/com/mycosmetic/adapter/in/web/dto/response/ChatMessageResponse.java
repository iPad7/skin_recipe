package com.mycosmetic.adapter.in.web.dto.response;

import com.mycosmetic.domain.chat.ChatMessage;
import com.mycosmetic.domain.chat.Role;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatMessageResponse {
    private final Long id;
    private final Role role;
    private final String content;
    private final LocalDateTime createdAt;

    public ChatMessageResponse(ChatMessage message) {
        this.id = message.getId();
        this.role = message.getRole();
        this.content = message.getContent();
        this.createdAt = message.getCreatedAt();
    }
}

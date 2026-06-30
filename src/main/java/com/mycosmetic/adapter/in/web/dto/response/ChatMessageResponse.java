package com.mycosmetic.adapter.in.web.dto.response;

import com.mycosmetic.application.chat.ChatMessageResult;
import com.mycosmetic.domain.chat.Role;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatMessageResponse {
    private final Long id;
    private final Role role;
    private final String content;
    private final LocalDateTime createdAt;

    private ChatMessageResponse(Long id, Role role, String content, LocalDateTime createdAt) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static ChatMessageResponse from(ChatMessageResult result) {
        return new ChatMessageResponse(result.id(), result.role(), result.content(), result.createdAt());
    }
}

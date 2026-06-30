package com.mycosmetic.adapter.in.web.dto.response;

import com.mycosmetic.application.chat.ChatResult;
import lombok.Getter;

@Getter
public class ChatResponse {
    private final String answer;

    private ChatResponse(String answer) {
        this.answer = answer;
    }

    public static ChatResponse from(ChatResult result) {
        return new ChatResponse(result.answer());
    }
}

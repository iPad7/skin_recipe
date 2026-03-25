package com.mycosmetic.dto.response;

import lombok.Getter;

@Getter
public class ChatResponse {
    private final String answer;

    public ChatResponse(String answer) {
        this.answer = answer;
    }
}

package com.mycosmetic.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ChatRequest {

    @NotBlank
    private String message;
}

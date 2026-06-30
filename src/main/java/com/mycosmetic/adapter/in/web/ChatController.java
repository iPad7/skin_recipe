package com.mycosmetic.adapter.in.web;

import com.mycosmetic.adapter.in.web.dto.request.ChatRequest;
import com.mycosmetic.adapter.in.web.dto.response.ChatMessageResponse;
import com.mycosmetic.adapter.in.web.dto.response.ChatResponse;
import com.mycosmetic.adapter.in.web.dto.response.ChatSessionResponse;
import com.mycosmetic.application.chat.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/sessions")
    public ResponseEntity<ChatSessionResponse> createSession(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(chatService.createSession(userDetails.getUsername()));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionResponse>> findAllSessions(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(chatService.findAllSessions(userDetails.getUsername()));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<ChatResponse> chat(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID sessionId,
            @Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.chat(userDetails.getUsername(), sessionId, request));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(chatService.getHistory(userDetails.getUsername(), sessionId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID sessionId) {
        chatService.deleteSession(userDetails.getUsername(), sessionId);
        return ResponseEntity.noContent().build();
    }
}

package com.mycosmetic.adapter.out.persistence;

import com.mycosmetic.application.port.out.ChatMessageRepository;
import com.mycosmetic.domain.chat.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChatMessagePersistenceAdapter implements ChatMessageRepository {

    private final ChatMessageJpaRepository jpa;

    @Override
    public ChatMessage save(ChatMessage message) {
        return jpa.save(message);
    }

    @Override
    public List<ChatMessage> findAllBySessionIdOrderByCreatedAtAsc(UUID sessionId) {
        return jpa.findAllBySessionIdOrderByCreatedAtAsc(sessionId);
    }
}

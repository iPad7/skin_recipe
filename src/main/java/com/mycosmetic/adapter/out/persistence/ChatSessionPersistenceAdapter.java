package com.mycosmetic.adapter.out.persistence;

import com.mycosmetic.application.port.out.ChatSessionRepository;
import com.mycosmetic.domain.chat.ChatSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChatSessionPersistenceAdapter implements ChatSessionRepository {

    private final ChatSessionJpaRepository jpa;

    @Override
    public ChatSession saveAndFlush(ChatSession session) {
        return jpa.saveAndFlush(session);
    }

    @Override
    public Optional<ChatSession> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<ChatSession> findAllByUserIdOrderByCreatedAtDesc(Long userId) {
        return jpa.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public void delete(ChatSession session) {
        jpa.delete(session);
    }

    @Override
    public void deleteAll(Iterable<? extends ChatSession> entities) {
        jpa.deleteAll(entities);
    }
}

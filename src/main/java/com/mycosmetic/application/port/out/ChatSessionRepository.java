package com.mycosmetic.application.port.out;

import com.mycosmetic.domain.chat.ChatSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ChatSession 영속성 outbound 포트.
 * 구현체: {@code adapter.out.persistence.ChatSessionPersistenceAdapter}
 */
public interface ChatSessionRepository {

    ChatSession saveAndFlush(ChatSession session);

    Optional<ChatSession> findById(UUID id);

    List<ChatSession> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    void delete(ChatSession session);

    void deleteAll(Iterable<? extends ChatSession> entities);
}

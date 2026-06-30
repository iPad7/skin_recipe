package com.mycosmetic.application.port.out;

import com.mycosmetic.domain.chat.ChatMessage;

import java.util.List;
import java.util.UUID;

/**
 * ChatMessage 영속성 outbound 포트.
 * 구현체: {@code adapter.out.persistence.ChatMessagePersistenceAdapter}
 */
public interface ChatMessageRepository {

    ChatMessage save(ChatMessage message);

    List<ChatMessage> findAllBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}

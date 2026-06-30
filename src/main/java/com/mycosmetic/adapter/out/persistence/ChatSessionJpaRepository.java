package com.mycosmetic.adapter.out.persistence;

import com.mycosmetic.domain.chat.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatSessionJpaRepository extends JpaRepository<ChatSession, UUID> {
    List<ChatSession> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}

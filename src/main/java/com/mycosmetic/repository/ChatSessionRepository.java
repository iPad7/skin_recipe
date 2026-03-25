package com.mycosmetic.repository;

import com.mycosmetic.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    List<ChatSession> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}

package com.mycosmetic.service;

import com.mycosmetic.dto.request.ChatRequest;
import com.mycosmetic.dto.response.ChatMessageResponse;
import com.mycosmetic.dto.response.ChatResponse;
import com.mycosmetic.dto.response.ChatSessionResponse;
import com.mycosmetic.entity.ChatMessage;
import com.mycosmetic.entity.ChatSession;
import com.mycosmetic.entity.Cosmetic;
import com.mycosmetic.entity.Role;
import com.mycosmetic.entity.User;
import com.mycosmetic.repository.ChatMessageRepository;
import com.mycosmetic.repository.ChatSessionRepository;
import com.mycosmetic.repository.CosmeticRepository;
import com.mycosmetic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CosmeticRepository cosmeticRepository;
    private final VectorStoreService vectorStoreService;
    private final UpstageLlmClient llmClient;
    private final UserRepository userRepository;

    @Transactional
    public ChatSessionResponse createSession(String email) {
        User user = getUser(email);
        ChatSession session = ChatSession.builder().user(user).build();
        chatSessionRepository.saveAndFlush(session);
        return new ChatSessionResponse(session);
    }

    public List<ChatSessionResponse> findAllSessions(String email) {
        User user = getUser(email);
        return chatSessionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(ChatSessionResponse::new)
                .toList();
    }

    @Transactional
    public void deleteSession(String email, UUID sessionId) {
        User user = getUser(email);
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));
        if (!session.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        chatSessionRepository.delete(session);
    }

    @Transactional
    public ChatResponse chat(String email, UUID sessionId, ChatRequest request) {
        User user = getUser(email);
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));
        if (!session.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        // 1. 관련 화장품 벡터 검색 (topK=5)
        List<Long> relatedIds = vectorStoreService.search(request.getMessage(), 5);
        List<Cosmetic> relatedCosmetics = relatedIds.isEmpty()
                ? List.of()
                : cosmeticRepository.findAllById(relatedIds);

        // 2. 시스템 프롬프트 조립 (피부 정보는 항상, 화장품 섹션은 조건부)
        String systemPrompt = buildSystemPrompt(user, relatedCosmetics);

        // 3. 최근 10개 메시지 로드
        List<ChatMessage> allMessages = chatMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId);
        int fromIndex = Math.max(0, allMessages.size() - 10);
        List<ChatMessage> history = allMessages.subList(fromIndex, allMessages.size());

        // 4. LLM 호출
        String answer = llmClient.chat(systemPrompt, history, request.getMessage());

        // 5. 질문 + 답변 저장
        chatMessageRepository.save(ChatMessage.builder()
                .session(session).role(Role.USER).content(request.getMessage()).build());
        chatMessageRepository.save(ChatMessage.builder()
                .session(session).role(Role.ASSISTANT).content(answer).build());

        return new ChatResponse(answer);
    }

    public List<ChatMessageResponse> getHistory(String email, UUID sessionId) {
        User user = getUser(email);
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));
        if (!session.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        return chatMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(ChatMessageResponse::new)
                .toList();
    }

    private String buildSystemPrompt(User user, List<Cosmetic> cosmetics) {
        StringBuilder sb = new StringBuilder();
        sb.append("당신은 피부 전문가 AI 어시스턴트입니다. 사용자의 피부 정보를 바탕으로 개인화된 스킨케어 조언을 제공합니다.\n\n");
        sb.append("[사용자 피부 정보]\n");
        sb.append("피부 타입: ").append(user.getSkinType()).append("\n");
        sb.append("피부 고민: ").append(user.getSkinConcerns()).append("\n");
        sb.append("알레르기 성분: ").append(user.getAllergyIngredients()).append("\n");

        if (!cosmetics.isEmpty()) {
            sb.append("\n[관련 보유 화장품]\n");
            for (Cosmetic c : cosmetics) {
                sb.append("- 제품명: ").append(c.getName())
                        .append(", 브랜드: ").append(c.getBrand())
                        .append(", 성분: ").append(c.getIngredients()).append("\n");
            }
        }

        return sb.toString();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }
}

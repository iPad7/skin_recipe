package com.mycosmetic.application.chat;

import com.mycosmetic.application.port.out.ChatMessageRepository;
import com.mycosmetic.application.port.out.ChatSessionRepository;
import com.mycosmetic.application.port.out.CosmeticRepository;
import com.mycosmetic.application.port.out.LlmPort;
import com.mycosmetic.application.port.out.RoutineRepository;
import com.mycosmetic.application.port.out.UserRepository;
import com.mycosmetic.application.port.out.VectorStorePort;
import com.mycosmetic.domain.chat.ChatMessage;
import com.mycosmetic.domain.chat.ChatSession;
import com.mycosmetic.domain.chat.Role;
import com.mycosmetic.domain.cosmetic.Cosmetic;
import com.mycosmetic.domain.routine.Routine;
import com.mycosmetic.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CosmeticRepository cosmeticRepository;
    private final RoutineRepository routineRepository;
    private final VectorStorePort vectorStoreService;
    private final LlmPort llmClient;
    private final UserRepository userRepository;

    @Transactional
    public ChatSessionResult createSession(String email) {
        User user = getUser(email);
        ChatSession session = ChatSession.builder().user(user).build();
        chatSessionRepository.saveAndFlush(session);
        return ChatSessionResult.from(session);
    }

    public List<ChatSessionResult> findAllSessions(String email) {
        User user = getUser(email);
        return chatSessionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(ChatSessionResult::from)
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
    public ChatResult chat(String email, UUID sessionId, ChatCommand command) {
        User user = getUser(email);
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));
        if (!session.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        // 1. 관련 화장품 벡터 검색 (topK=5) — 현재 유저 소유 화장품만 필터링
        List<Long> relatedIds = vectorStoreService.search(command.message(), 5);
        List<Cosmetic> relatedCosmetics = relatedIds.isEmpty()
                ? List.of()
                : cosmeticRepository.findAllById(relatedIds).stream()
                        .filter(c -> c.getUser().getId().equals(user.getId()))
                        .toList();

        // 2. 루틴 전체 조회 (JOIN FETCH로 N+1 방지)
        List<Routine> routines = routineRepository.findAllByUserIdWithCosmetics(user.getId());

        // 3. 시스템 프롬프트 조립 (피부 정보는 항상, 화장품·루틴 섹션은 조건부)
        String systemPrompt = buildSystemPrompt(user, relatedCosmetics, routines);

        // 4. 최근 10개 메시지 로드
        List<ChatMessage> allMessages = chatMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId);
        int fromIndex = Math.max(0, allMessages.size() - 10);
        List<ChatMessage> history = allMessages.subList(fromIndex, allMessages.size());

        // 5. LLM 호출
        String answer = llmClient.chat(systemPrompt, history, command.message());

        // 6. 질문 + 답변 저장
        chatMessageRepository.save(ChatMessage.builder()
                .session(session).role(Role.USER).content(command.message()).build());
        chatMessageRepository.save(ChatMessage.builder()
                .session(session).role(Role.ASSISTANT).content(answer).build());

        return new ChatResult(answer);
    }

    public List<ChatMessageResult> getHistory(String email, UUID sessionId) {
        User user = getUser(email);
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));
        if (!session.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        return chatMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(ChatMessageResult::from)
                .toList();
    }

    private String buildSystemPrompt(User user, List<Cosmetic> cosmetics, List<Routine> routines) {
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

        if (!routines.isEmpty()) {
            sb.append("\n[보유 루틴]\n");
            for (Routine r : routines) {
                String steps = r.getRoutineCosmetics().stream()
                        .map(rc -> rc.getCosmetic().getName())
                        .collect(Collectors.joining(" → "));
                sb.append("- ").append(r.getName())
                        .append(" (").append(r.getTimeOfDay()).append("): ")
                        .append(steps.isEmpty() ? "제품 없음" : steps).append("\n");
            }
        }

        return sb.toString();
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));
    }
}

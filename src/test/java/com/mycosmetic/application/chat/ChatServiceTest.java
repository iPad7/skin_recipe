package com.mycosmetic.application.chat;
import com.mycosmetic.application.port.out.VectorStoreService;
import com.mycosmetic.adapter.out.upstage.UpstageLlmClient;

import com.mycosmetic.adapter.in.web.dto.request.ChatRequest;
import com.mycosmetic.adapter.in.web.dto.response.ChatMessageResponse;
import com.mycosmetic.adapter.in.web.dto.response.ChatResponse;
import com.mycosmetic.adapter.in.web.dto.response.ChatSessionResponse;
import com.mycosmetic.domain.user.*;
import com.mycosmetic.domain.cosmetic.*;
import com.mycosmetic.domain.routine.*;
import com.mycosmetic.domain.chat.*;
import com.mycosmetic.adapter.out.persistence.ChatMessageRepository;
import com.mycosmetic.adapter.out.persistence.ChatSessionRepository;
import com.mycosmetic.adapter.out.persistence.CosmeticRepository;
import com.mycosmetic.adapter.out.persistence.RoutineRepository;
import com.mycosmetic.adapter.out.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @Mock private ChatSessionRepository chatSessionRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private CosmeticRepository cosmeticRepository;
    @Mock private RoutineRepository routineRepository;
    @Mock private VectorStoreService vectorStoreService;
    @Mock private UpstageLlmClient llmClient;
    @Mock private UserRepository userRepository;

    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private User user;
    private User otherUser;
    private ChatSession session;
    private Cosmetic cosmetic;

    @BeforeEach
    void setUp() {
        user = makeUser(1L, "user@example.com");
        otherUser = makeUser(2L, "other@example.com");
        session = makeChatSession(SESSION_ID, user);
        cosmetic = makeCosmetic(10L, user);
    }

    // ── 세션 생성 ──────────────────────────────────────────────────

    @Test
    @DisplayName("새 채팅 세션을 생성한다")
    void createSession_success() {
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));

        ChatSessionResponse response = chatService.createSession("user@example.com");

        assertThat(response).isNotNull();
        verify(chatSessionRepository).saveAndFlush(any(ChatSession.class));
    }

    // ── 세션 목록 조회 ─────────────────────────────────────────────

    @Test
    @DisplayName("내 채팅 세션 목록을 반환한다")
    void findAllSessions_returnsUserSessions() {
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(chatSessionRepository.findAllByUserIdOrderByCreatedAtDesc(1L))
                .willReturn(List.of(session));

        List<ChatSessionResponse> result = chatService.findAllSessions("user@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(SESSION_ID);
    }

    // ── 세션 삭제 ──────────────────────────────────────────────────

    @Test
    @DisplayName("내 채팅 세션을 삭제한다")
    void deleteSession_success() {
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(chatSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        chatService.deleteSession("user@example.com", SESSION_ID);

        verify(chatSessionRepository).delete(session);
    }

    @Test
    @DisplayName("존재하지 않는 세션 삭제 시 예외가 발생한다")
    void deleteSession_notFound_throwsException() {
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(chatSessionRepository.findById(SESSION_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.deleteSession("user@example.com", SESSION_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("세션을 찾을 수 없습니다");

        verify(chatSessionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("다른 사람의 세션을 삭제하면 예외가 발생한다")
    void deleteSession_notOwner_throwsException() {
        given(userRepository.findByEmail("other@example.com")).willReturn(Optional.of(otherUser));
        given(chatSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> chatService.deleteSession("other@example.com", SESSION_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("접근 권한");

        verify(chatSessionRepository, never()).delete(any());
    }

    // ── 챗봇 (RAG) ────────────────────────────────────────────────

    @Test
    @DisplayName("화장품이 있으면 RAG 파이프라인으로 답변을 반환한다")
    void chat_success_withCosmetics() {
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(chatSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
        given(vectorStoreService.search("추천해줘", 5)).willReturn(List.of(10L));
        given(cosmeticRepository.findAllById(List.of(10L))).willReturn(List.of(cosmetic));
        given(chatMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(SESSION_ID)).willReturn(List.of());
        given(llmClient.chat(any(), any(), any())).willReturn("토너를 추천합니다");

        ChatResponse response = chatService.chat("user@example.com", SESSION_ID, makeRequest("추천해줘"));

        assertThat(response.getAnswer()).isEqualTo("토너를 추천합니다");
        verify(chatMessageRepository, times(2)).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("화장품이 없어도 피부 정보만으로 답변하고 DB 화장품 조회는 생략된다")
    void chat_success_withoutCosmetics() {
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(chatSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
        given(vectorStoreService.search("추천해줘", 5)).willReturn(List.of());
        given(chatMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(SESSION_ID)).willReturn(List.of());
        given(llmClient.chat(any(), any(), any())).willReturn("피부 정보 기반 답변");

        ChatResponse response = chatService.chat("user@example.com", SESSION_ID, makeRequest("추천해줘"));

        assertThat(response.getAnswer()).isEqualTo("피부 정보 기반 답변");
        verify(cosmeticRepository, never()).findAllById(any());
        verify(chatMessageRepository, times(2)).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("대화 히스토리가 10개를 초과하면 최근 10개만 LLM에 전달된다")
    void chat_withLargeHistory_onlyLast10PassedToLlm() {
        List<ChatMessage> messages = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            messages.add(makeChatMessage(session, Role.USER, "메시지" + i));
        }

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(chatSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
        given(vectorStoreService.search(any(), anyInt())).willReturn(List.of());
        given(chatMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(SESSION_ID)).willReturn(messages);
        given(llmClient.chat(any(), any(), any())).willReturn("답변");

        chatService.chat("user@example.com", SESSION_ID, makeRequest("질문"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatMessage>> historyCaptor = ArgumentCaptor.forClass(List.class);
        verify(llmClient).chat(any(), historyCaptor.capture(), any());
        assertThat(historyCaptor.getValue()).hasSize(10);
    }

    @Test
    @DisplayName("존재하지 않는 세션으로 채팅 시 예외가 발생한다")
    void chat_sessionNotFound_throwsException() {
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(chatSessionRepository.findById(SESSION_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.chat("user@example.com", SESSION_ID, makeRequest("질문")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("세션을 찾을 수 없습니다");

        verify(llmClient, never()).chat(any(), any(), any());
    }

    @Test
    @DisplayName("다른 사람의 세션으로 채팅 시 예외가 발생한다")
    void chat_notOwner_throwsException() {
        given(userRepository.findByEmail("other@example.com")).willReturn(Optional.of(otherUser));
        given(chatSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> chatService.chat("other@example.com", SESSION_ID, makeRequest("질문")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("접근 권한");

        verify(llmClient, never()).chat(any(), any(), any());
    }

    @Test
    @DisplayName("루틴이 있으면 시스템 프롬프트에 루틴 정보가 포함된다")
    void chat_withRoutines_routineContextIncludedInPrompt() {
        Routine routine = makeRoutine(1L, "아침 루틴", TimeOfDay.AM, List.of(cosmetic));

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(chatSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
        given(vectorStoreService.search(any(), anyInt())).willReturn(List.of());
        given(routineRepository.findAllByUserIdWithCosmetics(1L)).willReturn(List.of(routine));
        given(chatMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(SESSION_ID)).willReturn(List.of());
        given(llmClient.chat(any(), any(), any())).willReturn("답변");

        chatService.chat("user@example.com", SESSION_ID, makeRequest("루틴 알려줘"));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient).chat(promptCaptor.capture(), any(), any());
        assertThat(promptCaptor.getValue()).contains("[보유 루틴]");
        assertThat(promptCaptor.getValue()).contains("아침 루틴");
        assertThat(promptCaptor.getValue()).contains("토너");
    }

    @Test
    @DisplayName("루틴이 없으면 시스템 프롬프트에 루틴 섹션이 포함되지 않는다")
    void chat_withoutRoutines_routineSectionAbsentFromPrompt() {
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(chatSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
        given(vectorStoreService.search(any(), anyInt())).willReturn(List.of());
        given(routineRepository.findAllByUserIdWithCosmetics(1L)).willReturn(List.of());
        given(chatMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(SESSION_ID)).willReturn(List.of());
        given(llmClient.chat(any(), any(), any())).willReturn("답변");

        chatService.chat("user@example.com", SESSION_ID, makeRequest("루틴 알려줘"));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient).chat(promptCaptor.capture(), any(), any());
        assertThat(promptCaptor.getValue()).doesNotContain("[보유 루틴]");
    }

    // ── 대화 히스토리 조회 ─────────────────────────────────────────

    @Test
    @DisplayName("세션의 대화 히스토리를 시간순으로 반환한다")
    void getHistory_success() {
        ChatMessage userMsg = makeChatMessage(session, Role.USER, "질문");
        ChatMessage assistantMsg = makeChatMessage(session, Role.ASSISTANT, "답변");

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(chatSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));
        given(chatMessageRepository.findAllBySessionIdOrderByCreatedAtAsc(SESSION_ID))
                .willReturn(List.of(userMsg, assistantMsg));

        List<ChatMessageResponse> result = chatService.getHistory("user@example.com", SESSION_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRole()).isEqualTo(Role.USER);
        assertThat(result.get(0).getContent()).isEqualTo("질문");
        assertThat(result.get(1).getRole()).isEqualTo(Role.ASSISTANT);
        assertThat(result.get(1).getContent()).isEqualTo("답변");
    }

    @Test
    @DisplayName("다른 사람의 세션 히스토리 조회 시 예외가 발생한다")
    void getHistory_notOwner_throwsException() {
        given(userRepository.findByEmail("other@example.com")).willReturn(Optional.of(otherUser));
        given(chatSessionRepository.findById(SESSION_ID)).willReturn(Optional.of(session));

        assertThatThrownBy(() -> chatService.getHistory("other@example.com", SESSION_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("접근 권한");
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────

    private User makeUser(Long id, String email) {
        try {
            User u = User.builder()
                    .email(email).password("encoded").nickname("테스터")
                    .skinType(SkinType.NORMAL).skinConcerns("ACNE").allergyIngredients("향료")
                    .build();
            setField(u, "id", id);
            return u;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ChatSession makeChatSession(UUID id, User owner) {
        try {
            ChatSession s = ChatSession.builder().user(owner).build();
            setField(s, "id", id);
            return s;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ChatMessage makeChatMessage(ChatSession s, Role role, String content) {
        return ChatMessage.builder().session(s).role(role).content(content).build();
    }

    private Cosmetic makeCosmetic(Long id, User owner) {
        try {
            Cosmetic c = Cosmetic.builder()
                    .user(owner).name("토너").brand("이니스프리")
                    .category(CosmeticCategory.SKIN).ingredients("정제수")
                    .build();
            setField(c, "id", id);
            return c;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Routine makeRoutine(Long id, String name, TimeOfDay timeOfDay, List<Cosmetic> cosmetics) {
        try {
            Routine r = Routine.builder()
                    .user(user).name(name).timeOfDay(timeOfDay).description("설명")
                    .build();
            setField(r, "id", id);
            for (int i = 0; i < cosmetics.size(); i++) {
                RoutineCosmetic rc = RoutineCosmetic.builder()
                        .routine(r).cosmetic(cosmetics.get(i)).order(i + 1)
                        .build();
                r.getRoutineCosmetics().add(rc);
            }
            return r;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ChatRequest makeRequest(String message) {
        try {
            ChatRequest req = new ChatRequest();
            setField(req, "message", message);
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                var field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}

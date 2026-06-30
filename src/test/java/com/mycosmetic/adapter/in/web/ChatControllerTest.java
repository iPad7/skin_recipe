package com.mycosmetic.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycosmetic.common.config.SecurityConfig;
import com.mycosmetic.adapter.in.web.dto.response.ChatMessageResponse;
import com.mycosmetic.adapter.in.web.dto.response.ChatResponse;
import com.mycosmetic.adapter.in.web.dto.response.ChatSessionResponse;
import com.mycosmetic.domain.user.*;
import com.mycosmetic.domain.cosmetic.*;
import com.mycosmetic.domain.routine.*;
import com.mycosmetic.domain.chat.*;
import com.mycosmetic.common.security.JwtUtil;
import com.mycosmetic.common.security.UserDetailsServiceImpl;
import com.mycosmetic.application.chat.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
@Import(SecurityConfig.class)
class ChatControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ChatService chatService;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private UserDetailsServiceImpl userDetailsService;

    private static final UUID SESSION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    // ── 인증 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("JWT 없이 접근하면 401을 반환한다")
    void unauthorized_withoutToken() throws Exception {
        mockMvc.perform(get("/chat/sessions"))
                .andExpect(status().isUnauthorized());
    }

    // ── 세션 생성 ──────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("새 채팅 세션을 생성한다")
    void createSession_success() throws Exception {
        given(chatService.createSession(anyString())).willReturn(makeChatSessionResponse());

        mockMvc.perform(post("/chat/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SESSION_ID.toString()));
    }

    // ── 세션 목록 조회 ─────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("내 채팅 세션 목록을 조회한다")
    void findAllSessions_success() throws Exception {
        given(chatService.findAllSessions(anyString())).willReturn(List.of(makeChatSessionResponse()));

        mockMvc.perform(get("/chat/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(SESSION_ID.toString()));
    }

    // ── 챗봇 ──────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("질문을 보내면 AI 답변을 반환한다")
    void chat_success() throws Exception {
        given(chatService.chat(anyString(), any(UUID.class), any()))
                .willReturn(new ChatResponse("토너를 추천합니다"));

        mockMvc.perform(post("/chat/sessions/{sessionId}/messages", SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", "추천해줘"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("토너를 추천합니다"));
    }

    @Test
    @WithMockUser
    @DisplayName("메시지가 빈 문자열이면 400을 반환한다")
    void chat_blankMessage_returns400() throws Exception {
        mockMvc.perform(post("/chat/sessions/{sessionId}/messages", SESSION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", ""))))
                .andExpect(status().isBadRequest());
    }

    // ── 히스토리 조회 ──────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("세션의 대화 히스토리를 조회한다")
    void getHistory_success() throws Exception {
        given(chatService.getHistory(anyString(), any(UUID.class)))
                .willReturn(List.of(makeChatMessageResponse(Role.USER, "질문")));

        mockMvc.perform(get("/chat/sessions/{sessionId}/messages", SESSION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("USER"))
                .andExpect(jsonPath("$[0].content").value("질문"));
    }

    // ── 세션 삭제 ──────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("채팅 세션을 삭제하면 204를 반환한다")
    void deleteSession_success() throws Exception {
        mockMvc.perform(delete("/chat/sessions/{sessionId}", SESSION_ID))
                .andExpect(status().isNoContent());
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────

    private ChatSessionResponse makeChatSessionResponse() {
        try {
            User user = User.builder()
                    .email("user@example.com").password("encoded").nickname("테스터")
                    .skinType(SkinType.NORMAL).build();
            ChatSession session = ChatSession.builder().user(user).build();
            setField(session, "id", SESSION_ID);
            setField(session, "createdAt", LocalDateTime.now());
            return new ChatSessionResponse(session);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ChatMessageResponse makeChatMessageResponse(Role role, String content) {
        try {
            User user = User.builder()
                    .email("user@example.com").password("encoded").nickname("테스터")
                    .skinType(SkinType.NORMAL).build();
            ChatSession session = ChatSession.builder().user(user).build();
            setField(session, "id", SESSION_ID);
            ChatMessage message = ChatMessage.builder()
                    .session(session).role(role).content(content).build();
            setField(message, "id", 1L);
            setField(message, "createdAt", LocalDateTime.now());
            return new ChatMessageResponse(message);
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

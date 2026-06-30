package com.mycosmetic.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycosmetic.common.config.SecurityConfig;
import com.mycosmetic.application.user.LoginResult;
import com.mycosmetic.common.security.JwtUtil;
import com.mycosmetic.common.security.UserDetailsServiceImpl;
import com.mycosmetic.application.user.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    // ── 회원가입 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("유효한 회원가입 요청이면 200과 메시지를 반환한다")
    void signup_success() throws Exception {
        Map<String, Object> body = Map.of(
                "email", "user@example.com",
                "password", "password123",
                "nickname", "홍길동",
                "skinType", "NORMAL"
        );

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."));
    }

    @Test
    @DisplayName("이메일 형식이 잘못되면 400을 반환한다")
    void signup_invalidEmail() throws Exception {
        Map<String, Object> body = Map.of(
                "email", "not-an-email",
                "password", "password123",
                "nickname", "홍길동",
                "skinType", "NORMAL"
        );

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("중복 이메일이면 400을 반환한다")
    void signup_duplicateEmail() throws Exception {
        willThrow(new IllegalArgumentException("이미 사용 중인 이메일입니다."))
                .given(authService).signup(any());

        Map<String, Object> body = Map.of(
                "email", "dup@example.com",
                "password", "password123",
                "nickname", "홍길동",
                "skinType", "NORMAL"
        );

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ── 로그인 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("올바른 로그인 요청이면 200과 accessToken을 반환한다")
    void login_success() throws Exception {
        given(authService.login(any())).willReturn(new LoginResult("mock-jwt-token"));

        Map<String, String> body = Map.of(
                "email", "user@example.com",
                "password", "password123"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-jwt-token"));
    }

    @Test
    @DisplayName("잘못된 자격증명이면 400을 반환한다")
    void login_wrongCredentials() throws Exception {
        willThrow(new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."))
                .given(authService).login(any());

        Map<String, String> body = Map.of(
                "email", "user@example.com",
                "password", "wrong"
        );

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}

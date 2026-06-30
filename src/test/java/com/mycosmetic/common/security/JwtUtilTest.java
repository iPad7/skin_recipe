package com.mycosmetic.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        // application.yml 없이 직접 값을 주입해 순수 단위 테스트로 실행
        jwtUtil = new JwtUtil(
                "test-secret-key-must-be-at-least-32-chars-long",
                86400000L
        );
    }

    @Test
    @DisplayName("토큰 생성 후 이메일 추출이 일치해야 한다")
    void generateAndExtractEmail() {
        String email = "test@example.com";

        String token = jwtUtil.generateToken(email);
        String extracted = jwtUtil.getEmailFromToken(token);

        assertThat(extracted).isEqualTo(email);
    }

    @Test
    @DisplayName("유효한 토큰은 검증을 통과해야 한다")
    void validateToken_valid() {
        String token = jwtUtil.generateToken("test@example.com");

        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("위변조된 토큰은 검증에 실패해야 한다")
    void validateToken_tampered() {
        String token = jwtUtil.generateToken("test@example.com");
        String tampered = token + "invalid";

        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("만료된 토큰은 검증에 실패해야 한다")
    void validateToken_expired() {
        JwtUtil shortLivedUtil = new JwtUtil(
                "test-secret-key-must-be-at-least-32-chars-long",
                -1L  // 이미 만료된 토큰
        );
        String token = shortLivedUtil.generateToken("test@example.com");

        assertThat(shortLivedUtil.validateToken(token)).isFalse();
    }
}

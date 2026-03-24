package com.mycosmetic.service;

import com.mycosmetic.dto.request.LoginRequest;
import com.mycosmetic.dto.request.SignupRequest;
import com.mycosmetic.dto.response.LoginResponse;
import com.mycosmetic.entity.SkinType;
import com.mycosmetic.entity.User;
import com.mycosmetic.repository.UserRepository;
import com.mycosmetic.security.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    // ── 회원가입 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("정상적인 회원가입 요청이면 User가 저장된다")
    void signup_success() {
        SignupRequest request = makeSignupRequest("user@example.com");
        given(userRepository.existsByEmail("user@example.com")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("encoded-password");

        authService.signup(request);

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("이미 존재하는 이메일이면 예외가 발생한다")
    void signup_duplicateEmail() {
        SignupRequest request = makeSignupRequest("dup@example.com");
        given(userRepository.existsByEmail("dup@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용 중인 이메일");

        verify(userRepository, never()).save(any());
    }

    // ── 로그인 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("올바른 이메일/비밀번호이면 JWT가 반환된다")
    void login_success() {
        LoginRequest request = makeLoginRequest("user@example.com", "password123");
        User user = makeUser("user@example.com", "encoded-password");

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123", "encoded-password")).willReturn(true);
        given(jwtUtil.generateToken("user@example.com")).willReturn("mock-jwt-token");

        LoginResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("mock-jwt-token");
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 예외가 발생한다")
    void login_emailNotFound() {
        LoginRequest request = makeLoginRequest("none@example.com", "password123");
        given(userRepository.findByEmail("none@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일 또는 비밀번호");
    }

    @Test
    @DisplayName("비밀번호가 틀리면 예외가 발생한다")
    void login_wrongPassword() {
        LoginRequest request = makeLoginRequest("user@example.com", "wrong");
        User user = makeUser("user@example.com", "encoded-password");

        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "encoded-password")).willReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일 또는 비밀번호");
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────

    private SignupRequest makeSignupRequest(String email) {
        // Lombok @Getter + @NoArgsConstructor 구조상 리플렉션으로 필드 직접 설정
        try {
            SignupRequest req = new SignupRequest();
            setField(req, "email", email);
            setField(req, "password", "password123");
            setField(req, "nickname", "테스터");
            setField(req, "skinType", SkinType.NORMAL);
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private LoginRequest makeLoginRequest(String email, String password) {
        try {
            LoginRequest req = new LoginRequest();
            setField(req, "email", email);
            setField(req, "password", password);
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private User makeUser(String email, String encodedPassword) {
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .nickname("테스터")
                .skinType(SkinType.NORMAL)
                .build();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

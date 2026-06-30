package com.mycosmetic.application.user;

import com.mycosmetic.domain.user.SkinType;
import com.mycosmetic.domain.user.User;
import com.mycosmetic.application.port.out.UserRepository;
import com.mycosmetic.common.security.JwtUtil;
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
        given(userRepository.existsByEmail("user@example.com")).willReturn(false);
        given(passwordEncoder.encode(any())).willReturn("encoded-password");

        authService.signup(signupCommand("user@example.com"));

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("이미 존재하는 이메일이면 예외가 발생한다")
    void signup_duplicateEmail() {
        given(userRepository.existsByEmail("dup@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.signup(signupCommand("dup@example.com")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용 중인 이메일");

        verify(userRepository, never()).save(any());
    }

    // ── 로그인 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("올바른 이메일/비밀번호이면 JWT가 반환된다")
    void login_success() {
        User user = makeUser("user@example.com", "encoded-password");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password123", "encoded-password")).willReturn(true);
        given(jwtUtil.generateToken("user@example.com")).willReturn("mock-jwt-token");

        LoginResult result = authService.login(new LoginCommand("user@example.com", "password123"));

        assertThat(result.accessToken()).isEqualTo("mock-jwt-token");
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 예외가 발생한다")
    void login_emailNotFound() {
        given(userRepository.findByEmail("none@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginCommand("none@example.com", "password123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일 또는 비밀번호");
    }

    @Test
    @DisplayName("비밀번호가 틀리면 예외가 발생한다")
    void login_wrongPassword() {
        User user = makeUser("user@example.com", "encoded-password");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong", "encoded-password")).willReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginCommand("user@example.com", "wrong")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일 또는 비밀번호");
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────

    private SignupCommand signupCommand(String email) {
        return new SignupCommand(email, "password123", "테스터", SkinType.NORMAL, null, null);
    }

    private User makeUser(String email, String encodedPassword) {
        return User.builder()
                .email(email)
                .password(encodedPassword)
                .nickname("테스터")
                .skinType(SkinType.NORMAL)
                .build();
    }
}

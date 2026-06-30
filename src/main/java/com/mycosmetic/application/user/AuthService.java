package com.mycosmetic.application.user;

import com.mycosmetic.application.port.out.ChatSessionRepository;
import com.mycosmetic.application.port.out.CosmeticRepository;
import com.mycosmetic.application.port.out.RoutineRepository;
import com.mycosmetic.application.port.out.UserRepository;
import com.mycosmetic.application.port.out.VectorStorePort;
import com.mycosmetic.common.security.JwtUtil;
import com.mycosmetic.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CosmeticRepository cosmeticRepository;
    private final RoutineRepository routineRepository;
    private final ChatSessionRepository chatSessionRepository;
    private final VectorStorePort vectorStoreService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public void signup(SignupCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .email(command.email())
                .password(passwordEncoder.encode(command.password()))
                .nickname(command.nickname())
                .skinType(command.skinType())
                .skinConcerns(command.skinConcerns())
                .allergyIngredients(command.allergyIngredients())
                .build();

        userRepository.save(user);
    }

    public LoginResult login(LoginCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(command.password(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return new LoginResult(token);
    }

    public UserResult getMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return UserResult.from(user);
    }

    @Transactional
    public UserResult updateMe(String email, UpdateUserCommand command) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.update(command.nickname(), command.skinType(),
                command.skinConcerns(), command.allergyIngredients());
        return UserResult.from(user);
    }

    @Transactional
    public void deleteMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        chatSessionRepository.deleteAll(
                chatSessionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()));

        routineRepository.deleteAll(
                routineRepository.findAllByUserId(user.getId()));

        var cosmetics = cosmeticRepository.findAllByUserId(user.getId());
        cosmetics.forEach(c -> vectorStoreService.removeVector(c.getId()));
        cosmeticRepository.deleteAll(cosmetics);

        userRepository.delete(user);
    }
}

package com.mycosmetic.service;

import com.mycosmetic.dto.request.LoginRequest;
import com.mycosmetic.dto.request.SignupRequest;
import com.mycosmetic.dto.request.UpdateUserRequest;
import com.mycosmetic.dto.response.LoginResponse;
import com.mycosmetic.dto.response.UserResponse;
import com.mycosmetic.entity.User;
import com.mycosmetic.repository.ChatSessionRepository;
import com.mycosmetic.repository.CosmeticRepository;
import com.mycosmetic.repository.RoutineRepository;
import com.mycosmetic.repository.UserRepository;
import com.mycosmetic.security.JwtUtil;
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
    private final VectorStoreService vectorStoreService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .skinType(request.getSkinType())
                .skinConcerns(request.getSkinConcerns())
                .allergyIngredients(request.getAllergyIngredients())
                .build();

        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return new LoginResponse(token);
    }

    public UserResponse getMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return new UserResponse(user);
    }

    @Transactional
    public UserResponse updateMe(String email, UpdateUserRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.update(request.getNickname(), request.getSkinType(),
                request.getSkinConcerns(), request.getAllergyIngredients());
        return new UserResponse(user);
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

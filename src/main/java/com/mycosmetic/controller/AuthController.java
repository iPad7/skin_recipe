package com.mycosmetic.controller;

import com.mycosmetic.dto.request.LoginRequest;
import com.mycosmetic.dto.request.SignupRequest;
import com.mycosmetic.dto.request.UpdateUserRequest;
import com.mycosmetic.dto.response.LoginResponse;
import com.mycosmetic.dto.response.MessageResponse;
import com.mycosmetic.dto.response.UserResponse;
import com.mycosmetic.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok(new MessageResponse("회원가입이 완료되었습니다."));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(authService.getMe(userDetails.getUsername()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@AuthenticationPrincipal UserDetails userDetails,
                                                 @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(authService.updateMe(userDetails.getUsername(), request));
    }
}

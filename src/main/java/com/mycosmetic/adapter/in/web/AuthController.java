package com.mycosmetic.adapter.in.web;

import com.mycosmetic.adapter.in.web.dto.request.LoginRequest;
import com.mycosmetic.adapter.in.web.dto.request.SignupRequest;
import com.mycosmetic.adapter.in.web.dto.request.UpdateUserRequest;
import com.mycosmetic.adapter.in.web.dto.response.LoginResponse;
import com.mycosmetic.adapter.in.web.dto.response.MessageResponse;
import com.mycosmetic.adapter.in.web.dto.response.UserResponse;
import com.mycosmetic.application.user.AuthService;
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

    @DeleteMapping("/me")
    public ResponseEntity<MessageResponse> deleteMe(@AuthenticationPrincipal UserDetails userDetails) {
        authService.deleteMe(userDetails.getUsername());
        return ResponseEntity.ok(new MessageResponse("회원 탈퇴가 완료되었습니다."));
    }
}

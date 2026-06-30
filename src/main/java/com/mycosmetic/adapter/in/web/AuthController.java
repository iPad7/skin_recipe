package com.mycosmetic.adapter.in.web;

import com.mycosmetic.adapter.in.web.dto.request.LoginRequest;
import com.mycosmetic.adapter.in.web.dto.request.SignupRequest;
import com.mycosmetic.adapter.in.web.dto.request.UpdateUserRequest;
import com.mycosmetic.adapter.in.web.dto.response.LoginResponse;
import com.mycosmetic.adapter.in.web.dto.response.MessageResponse;
import com.mycosmetic.adapter.in.web.dto.response.UserResponse;
import com.mycosmetic.application.user.AuthService;
import com.mycosmetic.application.user.LoginCommand;
import com.mycosmetic.application.user.LoginResult;
import com.mycosmetic.application.user.SignupCommand;
import com.mycosmetic.application.user.UpdateUserCommand;
import com.mycosmetic.application.user.UserResult;
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
        authService.signup(new SignupCommand(
                request.getEmail(), request.getPassword(), request.getNickname(),
                request.getSkinType(), request.getSkinConcerns(), request.getAllergyIngredients()));
        return ResponseEntity.ok(new MessageResponse("회원가입이 완료되었습니다."));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authService.login(new LoginCommand(request.getEmail(), request.getPassword()));
        return ResponseEntity.ok(new LoginResponse(result.accessToken()));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(UserResponse.from(authService.getMe(userDetails.getUsername())));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@AuthenticationPrincipal UserDetails userDetails,
                                                 @Valid @RequestBody UpdateUserRequest request) {
        UserResult result = authService.updateMe(userDetails.getUsername(), new UpdateUserCommand(
                request.getNickname(), request.getSkinType(),
                request.getSkinConcerns(), request.getAllergyIngredients()));
        return ResponseEntity.ok(UserResponse.from(result));
    }

    @DeleteMapping("/me")
    public ResponseEntity<MessageResponse> deleteMe(@AuthenticationPrincipal UserDetails userDetails) {
        authService.deleteMe(userDetails.getUsername());
        return ResponseEntity.ok(new MessageResponse("회원 탈퇴가 완료되었습니다."));
    }
}

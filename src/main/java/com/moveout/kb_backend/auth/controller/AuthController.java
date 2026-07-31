package com.moveout.kb_backend.auth.controller;

import com.moveout.kb_backend.auth.dto.LoginRequest;
import com.moveout.kb_backend.auth.dto.LoginResponse;
import com.moveout.kb_backend.auth.dto.SignupRequest;
import com.moveout.kb_backend.auth.dto.SignupResponse;
import com.moveout.kb_backend.auth.service.AuthService;
import com.moveout.kb_backend.common.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_HEADER = "Refresh-Token";

    private final AuthService authService;

    @PostMapping("/signup")
    public SignupResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request);
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + result.accessToken())
                .header(REFRESH_TOKEN_HEADER, result.refreshToken())
                .body(result.body());
    }

    @PostMapping("/reissue")
    public ResponseEntity<Void> reissue(@RequestHeader(REFRESH_TOKEN_HEADER) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException("AUTH_004", "유효하지 않은 토큰입니다.");
        }
        String accessToken = authService.reissue(refreshToken);
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + accessToken)
                .build();
    }
}

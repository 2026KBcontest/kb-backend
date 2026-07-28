package com.moveout.kb_backend.auth.service;

import com.moveout.kb_backend.auth.dto.LoginRequest;
import com.moveout.kb_backend.auth.dto.LoginResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public LoginResponse login(LoginRequest request) {
        // 프론트엔드 연동용 임시 성공 데이터 반환 (수요일 데모/통합용)
        String mockToken = "mock-jwt-token-kb-2026";
        return new LoginResponse(1L, request.getEmail(), "김국민", mockToken);
    }
}
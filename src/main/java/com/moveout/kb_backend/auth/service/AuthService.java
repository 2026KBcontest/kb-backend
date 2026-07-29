package com.moveout.kb_backend.auth.service;

import com.moveout.kb_backend.auth.dto.LoginRequest;
import com.moveout.kb_backend.auth.dto.LoginResponse;
import com.moveout.kb_backend.auth.dto.SignupRequest;
import com.moveout.kb_backend.auth.dto.SignupResponse;
import com.moveout.kb_backend.auth.jwt.JwtProvider;
import com.moveout.kb_backend.common.exception.BusinessException;
import com.moveout.kb_backend.user.entity.User;
import com.moveout.kb_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new BusinessException("AUTH_001", "이미 존재하는 아이디입니다.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("AUTH_002", "이미 존재하는 이메일입니다.");
        }

        User user = new User(
                request.getLoginId(),
                passwordEncoder.encode(request.getPassword()),
                request.getEmail(),
                request.getName());
        userRepository.save(user);

        return new SignupResponse(user.getId(), "회원가입이 완료되었습니다.");
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        User user = userRepository
                .findByLoginId(request.getLoginId())
                .orElseThrow(() -> new BusinessException("AUTH_003", "아이디 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("AUTH_003", "아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());
        user.updateRefreshToken(refreshToken);

        LoginResponse response = new LoginResponse(user.getId(), user.getLoginId(), user.getName());
        return new LoginResult(response, accessToken, refreshToken);
    }

    @Transactional
    public String reissue(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new BusinessException("AUTH_004", "유효하지 않은 토큰입니다.");
        }

        User user = userRepository
                .findById(jwtProvider.getUserId(refreshToken))
                .orElseThrow(() -> new BusinessException("AUTH_004", "유효하지 않은 토큰입니다."));

        if (!refreshToken.equals(user.getRefreshToken())) {
            throw new BusinessException("AUTH_004", "유효하지 않은 토큰입니다.");
        }

        return jwtProvider.createAccessToken(user.getId());
    }

    public record LoginResult(LoginResponse body, String accessToken, String refreshToken) {}
}

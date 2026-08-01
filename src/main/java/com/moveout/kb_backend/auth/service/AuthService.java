package com.moveout.kb_backend.auth.service;

import com.moveout.kb_backend.auth.dto.AgreementsRequest;
import com.moveout.kb_backend.auth.dto.LoginRequest;
import com.moveout.kb_backend.auth.dto.LoginResponse;
import com.moveout.kb_backend.auth.dto.SignupRequest;
import com.moveout.kb_backend.auth.dto.SignupResponse;
import com.moveout.kb_backend.auth.jwt.JwtProvider;
import com.moveout.kb_backend.common.exception.BusinessException;
import com.moveout.kb_backend.user.entity.User;
import com.moveout.kb_backend.user.entity.UserAgreement;
import com.moveout.kb_backend.user.repository.UserAgreementRepository;
import com.moveout.kb_backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.Period;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MIN_SIGNUP_AGE = 19;
    private static final int MAX_SIGNUP_AGE = 39;

    private final UserRepository userRepository;
    private final UserAgreementRepository userAgreementRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        int age = Period.between(request.getBirthDate(), LocalDate.now()).getYears();
        if (age < MIN_SIGNUP_AGE || age > MAX_SIGNUP_AGE) {
            throw new BusinessException("AUTH_006", "만 19~39세만 가입 가능합니다.");
        }
        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new BusinessException("AUTH_001", "이미 존재하는 아이디입니다.");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("AUTH_002", "이미 존재하는 이메일입니다.");
        }

        User user = User.builder()
                .loginId(request.getLoginId())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .name(request.getName())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .job(request.getJob())
                .residenceRegion(request.getResidenceRegion())
                .phone(request.getPhone())
                .build();
        userRepository.save(user);

        AgreementsRequest agreements = request.getAgreements();
        userAgreementRepository.save(new UserAgreement(
                user, agreements.getPrivacyAgreed(), agreements.getMydataAgreed(), agreements.getMarketingAgreed()));

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

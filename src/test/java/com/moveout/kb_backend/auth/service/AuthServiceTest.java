package com.moveout.kb_backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.moveout.kb_backend.auth.dto.AgreementsRequest;
import com.moveout.kb_backend.auth.dto.SignupRequest;
import com.moveout.kb_backend.auth.jwt.JwtProvider;
import com.moveout.kb_backend.common.exception.BusinessException;
import com.moveout.kb_backend.user.entity.Gender;
import com.moveout.kb_backend.user.entity.Job;
import com.moveout.kb_backend.user.repository.UserAgreementRepository;
import com.moveout.kb_backend.user.repository.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAgreementRepository userAgreementRepository;

    @Mock
    private JwtProvider jwtProvider;

    private AuthService authService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, userAgreementRepository, new BCryptPasswordEncoder(), jwtProvider);
    }

    private AgreementsRequest agreements() {
        return new AgreementsRequest(true, true, false);
    }

    @Test
    void signup_성공하면_UUID를_발급한다() {
        SignupRequest request = new SignupRequest(
                "testUser", "abcdefg!123", "test@example.com", "홍길동",
                LocalDate.of(1998, 1, 1), Gender.남성, Job.직장인, "서울특별시", "010-1234-5678", agreements());
        when(userRepository.existsByLoginId("testUser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(userRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.signup(request);

        assertThat(response.getUserId()).isNotNull();
    }

    @Test
    void signup_시_loginId가_중복이면_AUTH_001() {
        SignupRequest request = new SignupRequest(
                "dupUser", "abcdefg!123", "test@example.com", "홍길동",
                LocalDate.of(1998, 1, 1), Gender.남성, Job.직장인, "서울특별시", "010-1234-5678", agreements());
        when(userRepository.existsByLoginId("dupUser")).thenReturn(true);

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo("AUTH_001");
    }

    @Test
    void signup_시_만19세미만이면_AUTH_006() {
        SignupRequest request = new SignupRequest(
                "youngUser", "abcdefg!123", "test@example.com", "홍길동",
                LocalDate.now().minusYears(15), Gender.남성, Job.학생, "서울특별시", "010-1234-5678", agreements());

        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo("AUTH_006");
    }
}

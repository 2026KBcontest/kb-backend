package com.moveout.kb_backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moveout.kb_backend.common.exception.BusinessException;
import com.moveout.kb_backend.forecast.repository.SimulationResultRepository;
import com.moveout.kb_backend.mydata.repository.MyDataSnapshotRepository;
import com.moveout.kb_backend.user.dto.ChangePasswordRequest;
import com.moveout.kb_backend.user.dto.UpdateProfileRequest;
import com.moveout.kb_backend.user.entity.Gender;
import com.moveout.kb_backend.user.entity.Job;
import com.moveout.kb_backend.user.entity.User;
import com.moveout.kb_backend.user.repository.UserAgreementRepository;
import com.moveout.kb_backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MyDataSnapshotRepository myDataSnapshotRepository;

    @Mock
    private SimulationResultRepository simulationResultRepository;

    @Mock
    private UserAgreementRepository userAgreementRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository, myDataSnapshotRepository, simulationResultRepository, userAgreementRepository,
                passwordEncoder);
    }

    private User user() {
        return User.builder()
                .loginId("testUser")
                .password(passwordEncoder.encode("abcdefg!123"))
                .email("test@example.com")
                .name("홍길동")
                .birthDate(LocalDate.of(1998, 1, 1))
                .gender(Gender.남성)
                .job(Job.직장인)
                .residenceRegion("서울특별시")
                .phone("010-1234-5678")
                .build();
    }

    @Test
    void getMe_성공하면_사용자정보를_반환한다() {
        User user = user();
        user.updateMonthlyIncome(3_000_000L);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        var response = userService.getMe(user.getId());

        assertThat(response.loginId()).isEqualTo("testUser");
        assertThat(response.monthlyIncome()).isEqualTo(3_000_000L);
    }

    @Test
    void getMe_사용자가_없으면_USER_001() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getMe(userId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo("USER_001");
    }

    @Test
    void updateProfile_보낸_필드만_반영한다() {
        User user = user();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        UpdateProfileRequest request = new UpdateProfileRequest(null, null, null, "부산광역시", null, null, null);

        var response = userService.updateProfile(user.getId(), request);

        assertThat(response.residenceRegion()).isEqualTo("부산광역시");
        assertThat(response.name()).isEqualTo("홍길동");
    }

    @Test
    void updateProfile_다른사람이_쓰는_이메일이면_409_AUTH_002() {
        User user = user();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);
        UpdateProfileRequest request =
                new UpdateProfileRequest(null, "taken@example.com", null, null, null, null, null);

        BusinessException e = (BusinessException) org.assertj.core.api.Assertions
                .catchThrowable(() -> userService.updateProfile(user.getId(), request));

        assertThat(e.getErrorCode()).isEqualTo("AUTH_002");
        assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void changePassword_성공하면_refreshToken을_무효화한다() {
        User user = user();
        user.updateRefreshToken("old-refresh-token");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        ChangePasswordRequest request = new ChangePasswordRequest("abcdefg!123", "newPassword!123");

        userService.changePassword(user.getId(), request);

        assertThat(user.getRefreshToken()).isNull();
        assertThat(passwordEncoder.matches("newPassword!123", user.getPassword())).isTrue();
    }

    @Test
    void changePassword_현재비밀번호가_틀리면_401_AUTH_003() {
        User user = user();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        ChangePasswordRequest request = new ChangePasswordRequest("wrong-password!", "newPassword!123");

        BusinessException e = (BusinessException) org.assertj.core.api.Assertions
                .catchThrowable(() -> userService.changePassword(user.getId(), request));

        assertThat(e.getErrorCode()).isEqualTo("AUTH_003");
        assertThat(e.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void deleteAccount_연관된_데이터를_함께_삭제한다() {
        User user = user();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(myDataSnapshotRepository.findByUser(user)).thenReturn(Optional.empty());
        when(simulationResultRepository.findById(user.getId())).thenReturn(Optional.empty());
        when(userAgreementRepository.findById(user.getId())).thenReturn(Optional.empty());

        userService.deleteAccount(user.getId());

        verify(userRepository, times(1)).delete(user);
    }
}

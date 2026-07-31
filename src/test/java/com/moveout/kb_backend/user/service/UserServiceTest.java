package com.moveout.kb_backend.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.moveout.kb_backend.common.exception.BusinessException;
import com.moveout.kb_backend.user.entity.Gender;
import com.moveout.kb_backend.user.entity.Job;
import com.moveout.kb_backend.user.entity.User;
import com.moveout.kb_backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getMe_성공하면_사용자정보를_반환한다() {
        User user = User.builder()
                .loginId("testUser")
                .password("hashed")
                .email("test@example.com")
                .name("홍길동")
                .birthDate(LocalDate.of(1998, 1, 1))
                .gender(Gender.남성)
                .job(Job.직장인)
                .residenceRegion("서울특별시")
                .phone("010-1234-5678")
                .build();
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
}

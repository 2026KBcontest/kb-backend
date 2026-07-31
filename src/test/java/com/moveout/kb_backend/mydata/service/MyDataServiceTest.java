package com.moveout.kb_backend.mydata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.moveout.kb_backend.common.exception.BusinessException;
import com.moveout.kb_backend.mydata.entity.MyDataSnapshot;
import com.moveout.kb_backend.mydata.repository.MyDataSnapshotRepository;
import com.moveout.kb_backend.user.entity.Gender;
import com.moveout.kb_backend.user.entity.Job;
import com.moveout.kb_backend.user.entity.User;
import com.moveout.kb_backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class MyDataServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MyDataSnapshotRepository myDataSnapshotRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MyDataService myDataService;

    private User user() {
        return User.builder()
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
    }

    @Test
    void getSnapshot_연동내역이_있으면_스냅샷을_반환한다() {
        User user = user();
        MyDataSnapshot snapshot = new MyDataSnapshot(user);
        snapshot.updateAsset(1_000_000L, 500_000L, 200_000L, 300_000L, 100_000L);
        snapshot.updateConsumption(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(myDataSnapshotRepository.findByUser(user)).thenReturn(Optional.of(snapshot));

        var response = myDataService.getSnapshot(user.getId());

        assertThat(response.assetDeposit()).isEqualTo(1_000_000L);
    }

    @Test
    void getSnapshot_연동내역이_없으면_MYDATA_002() {
        User user = user();
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(myDataSnapshotRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> myDataService.getSnapshot(user.getId()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo("MYDATA_002");
    }
}

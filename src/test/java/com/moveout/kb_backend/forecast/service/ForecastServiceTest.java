package com.moveout.kb_backend.forecast.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.moveout.kb_backend.common.exception.BusinessException;
import com.moveout.kb_backend.forecast.dto.SetGoalRequest;
import com.moveout.kb_backend.forecast.entity.HousingType;
import com.moveout.kb_backend.forecast.region.RegionHousingFee;
import com.moveout.kb_backend.forecast.region.RegionHousingFeeLoader;
import com.moveout.kb_backend.forecast.repository.SimulationResultRepository;
import com.moveout.kb_backend.mydata.entity.MyDataSnapshot;
import com.moveout.kb_backend.mydata.repository.MyDataSnapshotRepository;
import com.moveout.kb_backend.user.entity.User;
import com.moveout.kb_backend.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ForecastServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MyDataSnapshotRepository myDataSnapshotRepository;

    @Mock
    private SimulationResultRepository simulationResultRepository;

    @Mock
    private RegionHousingFeeLoader regionHousingFeeLoader;

    private ForecastService forecastService;
    private User user;

    @BeforeEach
    void setUp() {
        forecastService = new ForecastService(
                userRepository, myDataSnapshotRepository, simulationResultRepository, regionHousingFeeLoader);
        user = new User("testUser", "hashed", "test@example.com", "홍길동");
        user.updateMonthlyIncome(3_000_000L);
    }

    @Test
    void 거래금액_5천만원_미만이면_상한요율_0_5퍼센트_한도_20만원() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(regionHousingFeeLoader.find("강북구")).thenReturn(Optional.of(new RegionHousingFee(0L, 100_000L)));
        when(myDataSnapshotRepository.findByUser(user)).thenReturn(Optional.empty());
        when(simulationResultRepository.findById(user.getId())).thenReturn(Optional.empty());

        SetGoalRequest request = requestOf("강북구", HousingType.WOLSE);
        var response = forecastService.simulate(user.getId(), request);

        // 거래금액 = 10,000,000 + 100,000*100 = 20,000,000 (<5천만 -> *70 재계산) = 17,000,000
        // 수수료 = min(17,000,000*5/1000, 200,000) = 85,000
        assertThat(response.brokerageFee()).isEqualTo(85_000L);
    }

    @Test
    void 거래금액_5천만_1억_미만이면_상한요율_0_4퍼센트_한도_30만원() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(regionHousingFeeLoader.find("강북구")).thenReturn(Optional.of(new RegionHousingFee(0L, 500_000L)));
        when(myDataSnapshotRepository.findByUser(user)).thenReturn(Optional.empty());
        when(simulationResultRepository.findById(user.getId())).thenReturn(Optional.empty());

        SetGoalRequest request = requestOf("강북구", HousingType.WOLSE);
        var response = forecastService.simulate(user.getId(), request);

        // 거래금액 = 10,000,000 + 500,000*100 = 60,000,000 (>=5천만, 재계산 없음)
        // 수수료 = min(60,000,000*4/1000, 300,000) = 240,000
        assertThat(response.brokerageFee()).isEqualTo(240_000L);
    }

    @Test
    void 거래금액_1억_이상이면_상한요율_0_3퍼센트_한도없음() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(regionHousingFeeLoader.find("서초구")).thenReturn(Optional.of(new RegionHousingFee(200_000_000L, 0L)));
        when(myDataSnapshotRepository.findByUser(user)).thenReturn(Optional.empty());
        when(simulationResultRepository.findById(user.getId())).thenReturn(Optional.empty());

        SetGoalRequest request = requestOf("서초구", HousingType.JEONSE);
        var response = forecastService.simulate(user.getId(), request);

        // 거래금액 = 200,000,000 (JEONSE 월세 0)
        // 수수료 = 200,000,000*3/1000 = 600,000 (한도 없음)
        assertThat(response.brokerageFee()).isEqualTo(600_000L);
    }

    @Test
    void 소득정보가_없으면_SIMULATION_002() {
        user.updateMonthlyIncome(null);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        SetGoalRequest request = requestOf("강북구", HousingType.JEONSE);

        assertThatThrownBy(() -> forecastService.simulate(user.getId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo("SIMULATION_002");
    }

    @Test
    void 자산스냅샷이_있으면_남은상환금액만_차감한다() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(regionHousingFeeLoader.find("강북구")).thenReturn(Optional.of(new RegionHousingFee(0L, 100_000L)));
        when(simulationResultRepository.findById(user.getId())).thenReturn(Optional.empty());

        MyDataSnapshot snapshot = new MyDataSnapshot(user);
        snapshot.updateAsset(1_000_000L, 500_000L, 200_000L, 300_000L, 100_000L);
        snapshot.updateConsumption(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);
        when(myDataSnapshotRepository.findByUser(user)).thenReturn(Optional.of(snapshot));

        var response = forecastService.simulate(user.getId(), requestOf("강북구", HousingType.WOLSE));

        // (1,000,000+500,000+200,000) - 100,000 = 1,600,000  (assetLoan 300,000은 차감 안 됨)
        assertThat(response.currentAsset()).isEqualTo(1_600_000L);
    }

    private SetGoalRequest requestOf(String region, HousingType housingType) {
        return new SetGoalRequest(region, housingType);
    }
}

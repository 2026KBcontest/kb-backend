package com.moveout.kb_backend.forecast.service;

import com.moveout.kb_backend.common.exception.BusinessException;
import com.moveout.kb_backend.forecast.dto.SetGoalRequest;
import com.moveout.kb_backend.forecast.dto.SimulationResultResponse;
import com.moveout.kb_backend.forecast.entity.HousingType;
import com.moveout.kb_backend.forecast.entity.SimulationResult;
import com.moveout.kb_backend.forecast.region.RegionHousingFee;
import com.moveout.kb_backend.forecast.region.RegionHousingFeeLoader;
import com.moveout.kb_backend.forecast.repository.SimulationResultRepository;
import com.moveout.kb_backend.mydata.entity.MyDataSnapshot;
import com.moveout.kb_backend.mydata.repository.MyDataSnapshotRepository;
import com.moveout.kb_backend.user.entity.User;
import com.moveout.kb_backend.user.repository.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ForecastService {

    private static final long WOLSE_FIXED_DEPOSIT = 10_000_000L;
    private static final long BRACKET_LOW = 50_000_000L;
    private static final long BRACKET_HIGH = 100_000_000L;
    private static final long BRACKET_LOW_CAP = 200_000L;
    private static final long BRACKET_MID_CAP = 300_000L;
    private static final double SAVING_FALLBACK_RATE = 0.2;

    private final UserRepository userRepository;
    private final MyDataSnapshotRepository myDataSnapshotRepository;
    private final SimulationResultRepository simulationResultRepository;
    private final RegionHousingFeeLoader regionHousingFeeLoader;

    @Transactional
    public SimulationResultResponse simulate(UUID userId, SetGoalRequest request) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException("SIMULATION_003", "존재하지 않는 사용자입니다."));

        Long monthlyIncome = user.getMonthlyIncome();
        if (monthlyIncome == null) {
            throw new BusinessException("SIMULATION_002", "소득 정보가 없습니다.");
        }

        RegionHousingFee fee = regionHousingFeeLoader
                .find(request.getRegion())
                .orElseThrow(() -> new BusinessException("SIMULATION_001", "지원하지 않는 지역입니다."));

        long deposit;
        long monthlyRent;
        if (request.getHousingType() == HousingType.JEONSE) {
            deposit = fee.depositWon();
            monthlyRent = 0L;
        } else {
            deposit = WOLSE_FIXED_DEPOSIT;
            monthlyRent = fee.monthlyRentWon();
        }

        long brokerageFee = calculateBrokerageFee(deposit, monthlyRent);
        long requiredAmount = deposit + monthlyRent * 2 + brokerageFee;

        Optional<MyDataSnapshot> snapshot = myDataSnapshotRepository.findByUser(user);
        long currentAsset = calculateCurrentAsset(snapshot);

        boolean fallbackApplied;
        long monthlySavingCapacity;
        if (snapshot.isEmpty()) {
            monthlySavingCapacity = applyFallback(monthlyIncome);
            fallbackApplied = true;
        } else {
            long raw = monthlyIncome - calculateTotalConsumption(snapshot.get());
            if (raw < 0) {
                monthlySavingCapacity = applyFallback(monthlyIncome);
                fallbackApplied = true;
            } else {
                monthlySavingCapacity = raw;
                fallbackApplied = false;
            }
        }

        Integer estimatedMonths;
        LocalDate predictedStartDate;
        if (currentAsset >= requiredAmount) {
            estimatedMonths = 0;
            predictedStartDate = LocalDate.now();
        } else if (monthlySavingCapacity <= 0) {
            estimatedMonths = null;
            predictedStartDate = null;
        } else {
            long remaining = requiredAmount - currentAsset;
            estimatedMonths = (int) Math.ceil((double) remaining / monthlySavingCapacity);
            predictedStartDate = LocalDate.now().plusMonths(estimatedMonths);
        }

        SimulationResult result =
                simulationResultRepository.findById(userId).orElseGet(() -> new SimulationResult(user));
        result.update(
                request.getRegion(),
                request.getHousingType(),
                deposit,
                monthlyRent,
                brokerageFee,
                requiredAmount,
                currentAsset,
                monthlySavingCapacity,
                fallbackApplied,
                estimatedMonths,
                predictedStartDate);
        simulationResultRepository.save(result);

        return SimulationResultResponse.from(result);
    }

    @Transactional(readOnly = true)
    public SimulationResultResponse getResult(UUID userId) {
        SimulationResult result = simulationResultRepository
                .findById(userId)
                .orElseThrow(() -> new BusinessException("SIMULATION_004", "저장된 시뮬레이션 결과가 없습니다."));
        return SimulationResultResponse.from(result);
    }

    private long calculateBrokerageFee(long deposit, long monthlyRent) {
        long baseAmount = deposit + monthlyRent * 100;
        long transactionAmount = baseAmount < BRACKET_LOW ? deposit + monthlyRent * 70 : baseAmount;

        if (transactionAmount < BRACKET_LOW) {
            return Math.min(transactionAmount * 5 / 1000, BRACKET_LOW_CAP);
        } else if (transactionAmount < BRACKET_HIGH) {
            return Math.min(transactionAmount * 4 / 1000, BRACKET_MID_CAP);
        } else {
            return transactionAmount * 3 / 1000;
        }
    }

    private long calculateCurrentAsset(Optional<MyDataSnapshot> snapshot) {
        if (snapshot.isEmpty()) {
            return 0L;
        }
        MyDataSnapshot s = snapshot.get();
        return (s.getAssetDeposit() + s.getAssetSaving() + s.getAssetInvestment())
                - s.getAssetRemainingRepayment();
    }

    private long calculateTotalConsumption(MyDataSnapshot s) {
        return s.getFood() + s.getCulture() + s.getShopping() + s.getEtc()
                + s.getFixedCostTransport() + s.getFixedCostTelecom() + s.getFixedCostInsurance()
                + s.getFixedCostSubscription() + s.getFixedCostLoanInterest() + s.getFixedCostHousing();
    }

    private long applyFallback(long monthlyIncome) {
        return (long) (monthlyIncome * SAVING_FALLBACK_RATE);
    }
}

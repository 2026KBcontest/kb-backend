package com.moveout.kb_backend.forecast.dto;

import com.moveout.kb_backend.forecast.entity.HousingType;
import com.moveout.kb_backend.forecast.entity.SimulationResult;
import java.time.LocalDate;

public record SimulationResultResponse(
        String region,
        HousingType housingType,
        Long deposit,
        Long monthlyRent,
        Long brokerageFee,
        Long requiredAmount,
        Long currentAsset,
        Long monthlySavingCapacity,
        boolean isFallbackApplied,
        Integer estimatedMonths,
        LocalDate predictedStartDate,
        /* 이 분석을 마지막으로 돌린 시각.
           화면의 '마지막 분석일' 이 이 값을 쓴다. 없으면 새 기기에서 로그인했을 때
           분석 결과는 있는데 언제 한 건지 모르는 상태가 된다. */
        java.time.LocalDateTime updatedAt) {

    public static SimulationResultResponse from(SimulationResult result) {
        return new SimulationResultResponse(
                result.getRegion(),
                result.getHousingType(),
                result.getDeposit(),
                result.getMonthlyRent(),
                result.getBrokerageFee(),
                result.getRequiredAmount(),
                result.getCurrentAsset(),
                result.getMonthlySavingCapacity(),
                result.isFallbackApplied(),
                result.getEstimatedMonths(),
                result.getPredictedStartDate(),
                result.getUpdatedAt());
    }
}

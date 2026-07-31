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
        LocalDate predictedStartDate) {

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
                result.getPredictedStartDate());
    }
}

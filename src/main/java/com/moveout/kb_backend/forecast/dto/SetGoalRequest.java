package com.moveout.kb_backend.forecast.dto;

import com.moveout.kb_backend.forecast.entity.HousingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SetGoalRequest {

    @NotBlank
    private String region;

    @NotNull
    private HousingType housingType;
}

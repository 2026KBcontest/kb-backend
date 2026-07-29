package com.moveout.kb_backend.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateIncomeRequest {

    @NotNull
    @PositiveOrZero
    private Long monthlyIncome;
}

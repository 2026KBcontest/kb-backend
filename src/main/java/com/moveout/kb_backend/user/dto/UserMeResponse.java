package com.moveout.kb_backend.user.dto;

import com.moveout.kb_backend.user.entity.Gender;
import com.moveout.kb_backend.user.entity.Job;
import com.moveout.kb_backend.user.entity.User;
import java.time.LocalDate;

public record UserMeResponse(
        String loginId,
        String name,
        String email,
        Long monthlyIncome,
        Long monthlySavingGoal,
        LocalDate birthDate,
        String residenceRegion,
        Gender gender,
        Job job) {

    public static UserMeResponse from(User user) {
        return new UserMeResponse(
                user.getLoginId(),
                user.getName(),
                user.getEmail(),
                user.getMonthlyIncome(),
                user.getMonthlySavingGoal(),
                user.getBirthDate(),
                user.getResidenceRegion(),
                user.getGender(),
                user.getJob());
    }
}

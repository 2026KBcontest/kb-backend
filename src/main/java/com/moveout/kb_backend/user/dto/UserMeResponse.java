package com.moveout.kb_backend.user.dto;

import com.moveout.kb_backend.user.entity.User;

public record UserMeResponse(String loginId, String name, String email, Long monthlyIncome) {

    public static UserMeResponse from(User user) {
        return new UserMeResponse(user.getLoginId(), user.getName(), user.getEmail(), user.getMonthlyIncome());
    }
}

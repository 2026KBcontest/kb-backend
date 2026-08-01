package com.moveout.kb_backend.user.dto;

import com.moveout.kb_backend.user.entity.Gender;
import com.moveout.kb_backend.user.entity.Job;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    private String name;

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @Past(message = "생년월일은 과거 날짜여야 합니다.")
    private LocalDate birthDate;

    private String residenceRegion;

    private Gender gender;

    private Job job;

    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
    private String phone;

    /** 월 저축 목표 (원). 보내지 않으면(null) 기존 값을 유지한다. */
    @PositiveOrZero(message = "월 저축 목표는 0원 이상이어야 합니다.")
    private Long monthlySavingGoal;
}

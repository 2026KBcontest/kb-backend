package com.moveout.kb_backend.user.dto;

import com.moveout.kb_backend.user.entity.Gender;
import com.moveout.kb_backend.user.entity.Job;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
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
}

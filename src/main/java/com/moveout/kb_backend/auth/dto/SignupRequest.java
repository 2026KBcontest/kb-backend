package com.moveout.kb_backend.auth.dto;

import com.moveout.kb_backend.user.entity.Gender;
import com.moveout.kb_backend.user.entity.Job;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9]{4,20}$", message = "아이디는 영문 대소문자와 숫자 4~20자여야 합니다.")
    private String loginId;

    @NotBlank
    @Size(min = 10, max = 22, message = "비밀번호는 10~22자여야 합니다.")
    @Pattern(regexp = ".*[^a-zA-Z0-9].*", message = "비밀번호는 특수문자를 최소 1개 포함해야 합니다.")
    private String password;

    @NotBlank
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    @NotBlank
    private String name;

    @NotNull
    @Past(message = "생년월일은 과거 날짜여야 합니다.")
    private LocalDate birthDate;

    @NotNull
    private Gender gender;

    @NotNull
    private Job job;

    @NotBlank
    private String residenceRegion;

    @NotBlank
    @Pattern(regexp = "^010-\\d{4}-\\d{4}$", message = "휴대폰 번호 형식이 올바르지 않습니다. (예: 010-1234-5678)")
    private String phone;
}

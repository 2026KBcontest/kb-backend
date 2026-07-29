package com.moveout.kb_backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z]{4,20}$", message = "아이디는 영문 대소문자 4~20자여야 합니다.")
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
}

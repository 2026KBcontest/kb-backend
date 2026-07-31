package com.moveout.kb_backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    @NotBlank
    private String currentPassword;

    @NotBlank
    @Size(min = 10, max = 22, message = "비밀번호는 10~22자여야 합니다.")
    @Pattern(regexp = ".*[^a-zA-Z0-9].*", message = "비밀번호는 특수문자를 최소 1개 포함해야 합니다.")
    private String newPassword;
}

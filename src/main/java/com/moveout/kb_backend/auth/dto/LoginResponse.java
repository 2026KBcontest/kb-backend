package com.moveout.kb_backend.auth.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private UUID userId;
    private String loginId;
    private String name;
}

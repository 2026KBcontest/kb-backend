package com.moveout.kb_backend.policy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyResponse {

    private List<PolicyItemDto> policies;
    private String aiReason;
}
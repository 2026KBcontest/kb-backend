package com.moveout.kb_backend.policy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PolicyResponse {

    private String policyName; // 정책명
    private String description; // 정책 설명
    private String eligibility; // 지원 조건
    private String link; // 신청 링크
}
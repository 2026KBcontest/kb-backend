package com.moveout.kb_backend.policy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyItemDto {
    private String policyId;    // 고유 ID
    private String policyName;  // 정책명
    private String description; // 25자 이내 한 줄 설명
    private String eligibility; // 자격 요건
    private String status;      // "신청 가능" 또는 "조건 확인 필요"
    private String link;        // 신청 URL
}
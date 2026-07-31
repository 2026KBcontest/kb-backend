package com.moveout.kb_backend.policy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyItemDto {
    private String policyId;
    private String name;
    private String category;
    private String description;
    private String status;
    private String link;
    
    // [추가] 지원 금액 (원 단위, 미정/모를 경우 null)
    private Integer supportAmount;
    
    // [추가] 계산 근거 한 줄 (예: "월 20만원 × 12개월")
    private String supportNote;
}
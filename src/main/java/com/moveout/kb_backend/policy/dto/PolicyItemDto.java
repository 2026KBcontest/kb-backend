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

    /** 신청 대상 조건 (예: "만 19~39세 · 서울 거주"). 정책 자체의 성격이라 사용자와 무관하게 고정. */
    private String eligibility;

    /**
     * 이 사용자에게 왜 맞는지 (예: "만 27세 — 대상 연령에 들어가요").
     *
     * <p>서버가 필터를 통과시킨 근거를 그대로 적는다. AI 가 지어낸 말이 아니라
     * "우리가 어떤 조건으로 걸렀는지" 를 사용자에게 보여주는 것이다.
     * 조건을 몰라서 판단할 수 없으면 빈 목록.
     */
    private java.util.List<String> matchReasons;
}
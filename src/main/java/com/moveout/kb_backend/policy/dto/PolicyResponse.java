package com.moveout.kb_backend.policy.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class PolicyResponse {
    private List<PolicyItemDto> policies; // 정책 0건일 때는 빈 배열 [] 할당
    private RecommendationDto recommendation; // AI 실패 시 또는 정책 0건일 때 null 할당
}
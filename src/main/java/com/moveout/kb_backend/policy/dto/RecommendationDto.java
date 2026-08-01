package com.moveout.kb_backend.policy.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class RecommendationDto {
    private String source;      // "ai" 고정
    private String pick;        // 추천 정책 이름
    private String headline;    // 추천 헤드라인 문구
    private List<ReasonDto> reasons; // 추천 사유 리스트
    private String alternative; // 대안/조언 문구

    @Getter
    @Builder
    public static class ReasonDto {
        private String label; // 예: "나이"
        private String value; // 예: "만 27세"
        private String note;  // 예: "지원 대상 만 19~39세에 해당"
    }
}
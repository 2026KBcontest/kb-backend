package com.moveout.kb_backend.policy.service;

import com.moveout.kb_backend.policy.dto.PolicyItemDto;
import com.moveout.kb_backend.policy.dto.PolicyRequest;
import com.moveout.kb_backend.policy.dto.PolicyResponse;
import com.moveout.kb_backend.policy.dto.RecommendationDto;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class PolicyService {

    public PolicyResponse getRecommendedPolicies(PolicyRequest request) {
        // [시연용 Fallback 데이터 4건 확장]
        List<PolicyItemDto> fallbackPolicies = List.of(
            PolicyItemDto.builder()
                .policyId("youth-rent-01")
                .name("서울시 청년 월세 지원")
                .category("HOUSING")
                .description("청년층의 주거비 부담 완화를 위해 월세를 지원합니다.")
                .status("신청 가능")
                .link("https://youth.seoul.go.kr")
                .supportAmount(2400000)
                .supportNote("월 20만원 × 12개월")
                .build(),
            PolicyItemDto.builder()
                .policyId("youth-jeonse-02")
                .name("청년 버팀목 전세자금대출")
                .category("LOAN")
                .description("무주택 청년 전세보증금 저리 대출 지원 서비스입니다.")
                .status("신청 가능")
                .link("https://nhuf.molit.go.kr")
                .supportAmount(null)
                .supportNote("대출 한도 우대 (최대 2억원)")
                .build(),
            PolicyItemDto.builder()
                .policyId("youth-savings-03")
                .name("청년도약계좌 정부기여금")
                .category("SAVINGS")
                .description("만기 5년 동안 매월 납입금에 비례해 정부기여금을 지원합니다.")
                .status("조건 확인 필요")
                .link("https://kinfa.or.kr")
                .supportAmount(1440000)
                .supportNote("최대 월 2.4만원 × 60개월")
                .build(),
            PolicyItemDto.builder()
                .policyId("youth-transport-04")
                .name("K-패스 청년 대중교통비 환급")
                .category("TRANSPORT")
                .description("대중교통 이용 금액의 30%를 적립 및 환급해 드립니다.")
                .status("신청 가능")
                .link("https://korea-pass.kr")
                .supportAmount(360000)
                .supportNote("월 평균 3만원 환급 기준")
                .build()
        );

        // 조건 검색 결과가 없을 경우 (404 대신 빈 배열과 null 반환)
        if (fallbackPolicies == null || fallbackPolicies.isEmpty()) {
            return PolicyResponse.builder()
                .policies(Collections.emptyList())
                .recommendation(null)
                .build();
        }

        // 임시 AI 추천 규격 데이터 예시
        RecommendationDto recommendation = RecommendationDto.builder()
            .source("ai")
            .pick("서울시 청년 월세 지원")
            .headline("서울시 청년 월세 지원을 먼저 신청해보세요.")
            .reasons(List.of(
                RecommendationDto.ReasonDto.builder()
                    .label("지역")
                    .value(request.getResidenceRegion() != null ? request.getResidenceRegion() : "서울특별시")
                    .note("지원 대상 지역 요건 충족")
                    .build(),
                RecommendationDto.ReasonDto.builder()
                    .label("소득")
                    .value("월 소득 요건")
                    .note("중위소득 범위 내 해당")
                    .build()
            ))
            .alternative("버팀목 전세자금대출은 보증금이 모인 후 신청하는 것이 유리합니다.")
            .build();

        return PolicyResponse.builder()
            .policies(fallbackPolicies)
            .recommendation(recommendation)
            .build();
    }
}
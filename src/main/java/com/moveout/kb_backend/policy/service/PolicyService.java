package com.moveout.kb_backend.policy.service;

import com.moveout.kb_backend.policy.dto.PolicyItemDto;
import com.moveout.kb_backend.policy.dto.PolicyRequest;
import com.moveout.kb_backend.policy.dto.PolicyResponse;
import com.moveout.kb_backend.policy.dto.RecommendationDto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
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

        // [추가] 요청으로 받은 지역·생년월일을 실제 필터로 사용한다.
        // 이 줄이 없으면 어느 지역, 몇 살이 요청해도 위 4건이 그대로 나간다.
        Integer age = calcAge(request.getBirthDate());
        List<PolicyItemDto> policies = fallbackPolicies.stream()
            .filter(policy -> matches(policy, request.getResidenceRegion(), age))
            .toList();

        // 조건 검색 결과가 없을 경우 (404 대신 빈 배열과 null 반환)
        if (policies.isEmpty()) {
            return PolicyResponse.builder()
                .policies(Collections.emptyList())
                .recommendation(null)
                .build();
        }

        // 임시 AI 추천 규격 데이터 예시
        // [추가] 걸러진 목록 안에서 고른다. 이름을 고정해두면 그 정책이 필터에 걸렸을 때
        //        목록에 없는 정책을 추천하게 된다.
        PolicyItemDto best = policies.stream()
            .filter(policy -> "신청 가능".equals(policy.getStatus()))
            .findFirst()
            .orElse(policies.get(0));

        RecommendationDto recommendation = RecommendationDto.builder()
            // [수정] "ai" → "rule". 아직 AI 를 호출하지 않는데 "ai" 라고 주면
            //        화면에 'AI 분석' 배지가 잘못 붙는다. 연동이 끝나면 "ai" 로 되돌린다.
            .source("rule")
            .pick(best.getName())
            .headline(best.getName() + "을(를) 먼저 확인해보세요.")
            .reasons(List.of(
                RecommendationDto.ReasonDto.builder()
                    .label("지역")
                    .value(request.getResidenceRegion() != null ? request.getResidenceRegion() : "전국")
                    .note("지원 대상 지역 요건 충족")
                    .build(),
                RecommendationDto.ReasonDto.builder()
                    .label("나이")
                    .value(age != null ? "만 " + age + "세" : "확인 필요")
                    .note("청년 연령 요건 충족")
                    .build()
            ))
            // 소득 기준은 정책마다 달라 지금 판단할 수 없다. 단정하지 않고 안내만 한다.
            .alternative(policies.size() > 1
                ? policies.get(1).getName() + "도 함께 확인해보세요. 소득 기준은 신청 시 확인이 필요합니다."
                : "소득 기준은 정책마다 달라 신청 시 확인이 필요합니다.")
            .build();

        return PolicyResponse.builder()
            .policies(policies)
            .recommendation(recommendation)
            .build();
    }

    // [추가] 생년월일 → 만 나이. 값이 없거나 형식이 이상하면 null 로 두고 나이 조건을 적용하지 않는다.
    private Integer calcAge(String birthDate) {
        if (birthDate == null || birthDate.isBlank()) {
            return null;
        }
        try {
            return Period.between(LocalDate.parse(birthDate), LocalDate.now()).getYears();
        } catch (Exception e) {
            return null;
        }
    }

    // [추가] 정책별 신청 요건. 값이 null 이면 그 조건은 건너뛴다(있는 정보로만 거른다).
    private boolean matches(PolicyItemDto policy, String residenceRegion, Integer age) {
        // 청년 정책 공통 연령 요건 (만 19~39세)
        if (age != null && (age < 19 || age > 39)) {
            return false;
        }
        // 서울시 사업은 서울 거주자만 신청할 수 있다
        if ("youth-rent-01".equals(policy.getPolicyId())
            && residenceRegion != null && !residenceRegion.startsWith("서울")) {
            return false;
        }
        // 버팀목 전세자금대출·청년도약계좌는 만 19~34세 대상
        if (age != null && age > 34
            && ("youth-jeonse-02".equals(policy.getPolicyId())
                || "youth-savings-03".equals(policy.getPolicyId()))) {
            return false;
        }
        return true;
    }
}
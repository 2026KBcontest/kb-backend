package com.moveout.kb_backend.policy.service;

import com.moveout.kb_backend.policy.dto.PolicyItemDto;
import com.moveout.kb_backend.policy.dto.PolicyRequest;
import com.moveout.kb_backend.policy.dto.PolicyResponse;
import com.moveout.kb_backend.policy.dto.RecommendationDto;
import com.moveout.kb_backend.ai.service.AiPicker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final AiPicker aiPicker;

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
                .eligibility("만 19~39세 · 서울 · 무주택")
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
                .eligibility("만 19~34세 · 전국 · 무주택")
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
                .eligibility("만 19~34세 · 전국 · 소득 요건 있음")
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
                .eligibility("만 19~39세 · 전국 · 추가 조건 없음")
                .build()
        );

        // [추가] 요청으로 받은 지역·생년월일을 실제 필터로 사용한다.
        // 이 줄이 없으면 어느 지역, 몇 살이 요청해도 위 4건이 그대로 나간다.
        Integer age = calcAge(request.getBirthDate());
        List<PolicyItemDto> policies = fallbackPolicies.stream()
            .filter(policy -> matches(policy, request.getResidenceRegion(), age))
            // 왜 이 정책이 남았는지를 카드에 적어준다. 걸러낸 근거를 그대로 보여주는 것이다.
            .map(policy -> withMatchReasons(policy, request.getResidenceRegion(), age))
            .toList();

        // 조건 검색 결과가 없을 경우 (404 대신 빈 배열과 null 반환)
        if (policies.isEmpty()) {
            return PolicyResponse.builder()
                .policies(Collections.emptyList())
                .recommendation(null)
                .build();
        }

        // AI 가 고르게 해보고, 실패하면 규칙 기반으로 내려간다. 화면은 어느 쪽이든 동작한다.
        RecommendationDto recommendation = recommendByAi(request, age, policies);
        if (recommendation == null) {
            recommendation = recommendByRule(request, age, policies);
        }

        return PolicyResponse.builder()
            .policies(policies)
            .recommendation(recommendation)
            .build();
    }

    /**
     * AI 추천.
     *
     * <p>정책 목록을 표처럼 적어 프롬프트에 넣고, 그 안에서만 고르게 한다.
     * 지원 금액은 우리가 계산한 값을 그대로 적어 보내므로 AI 가 금액을 지어낼 여지가 없다.
     *
     * @return 호출 실패하거나 목록 밖을 고르면 null
     */
    private RecommendationDto recommendByAi(PolicyRequest request, Integer age, List<PolicyItemDto> policies) {
        String userBlock = """
            나이 : %s
            월 소득 : %s
            거주 지역 : %s
            """.formatted(
                age != null ? "만 " + age + "세" : "확인 필요",
                request.getIncome() != null ? String.format("%,d원", request.getIncome()) : "확인 필요",
                request.getResidenceRegion() != null ? request.getResidenceRegion() : "확인 필요");

        List<String> lines = new ArrayList<>();
        for (PolicyItemDto policy : policies) {
            lines.add(String.format(
                "id=%s | %s | %s | 지원금 %s | %s | %s",
                policy.getPolicyId(),
                policy.getName(),
                policy.getCategory(),
                policy.getSupportAmount() != null
                    ? String.format("%,d원", policy.getSupportAmount())
                    : "금액 미정",
                policy.getSupportNote() != null ? policy.getSupportNote() : "-",
                policy.getStatus()));
        }

        List<String> ids = policies.stream().map(PolicyItemDto::getPolicyId).toList();

        AiPicker.Pick pick = aiPicker.pick(
            userBlock,
            lines,
            ids,
            "위 정책 중 이 사용자가 가장 먼저 확인하면 좋을 것 하나를 고르고, 왜 그런지 설명해주세요.");

        if (pick == null) {
            return null;
        }

        // pickId 는 policyId 인데 화면에는 이름이 나가야 한다
        String pickName = policies.stream()
            .filter(policy -> policy.getPolicyId().equals(pick.pickId()))
            .map(PolicyItemDto::getName)
            .findFirst()
            .orElse(pick.pickId());

        String altName = pick.altId() == null ? null : policies.stream()
            .filter(policy -> policy.getPolicyId().equals(pick.altId()))
            .map(PolicyItemDto::getName)
            .findFirst()
            .orElse(null);

        // AI 가 쓴 이유 문장은 label/value 로 나눌 수 없어 note 에만 담는다
        List<RecommendationDto.ReasonDto> reasons = pick.reasons().stream()
            .map(text -> RecommendationDto.ReasonDto.builder().note(text).build())
            .toList();

        return RecommendationDto.builder()
            .source("ai")
            .pick(pickName)
            .headline(pick.headline())
            .reasons(reasons)
            .alternative(altName != null ? altName + "도 함께 확인해보세요." : null)
            .build();
    }

    /** AI 가 없을 때 쓰는 추천. source 를 "rule" 로 정직하게 표기한다. */
    private RecommendationDto recommendByRule(PolicyRequest request, Integer age, List<PolicyItemDto> policies) {
        // 걸러진 목록 안에서 고른다. 이름을 고정해두면 그 정책이 필터에 걸렸을 때
        // 목록에 없는 정책을 추천하게 된다.
        PolicyItemDto best = policies.stream()
            .filter(policy -> "신청 가능".equals(policy.getStatus()))
            .findFirst()
            .orElse(policies.get(0));

        return RecommendationDto.builder()
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

    /**
     * 이 정책이 왜 이 사용자에게 맞는지 한 줄씩 적는다.
     *
     * <p>AI 가 지어낸 설명이 아니라 {@link #matches} 가 통과시킨 근거 그대로다.
     * "왜 나한테 이걸 추천했지?" 는 사용자가 가장 먼저 하는 질문이고,
     * 답이 없으면 추천 자체를 안 믿게 된다.
     */
    private PolicyItemDto withMatchReasons(PolicyItemDto policy, String residenceRegion, Integer age) {
        /* 카드마다 문장 모양이 다르면 눈이 매번 새로 읽어야 한다.
           "라벨 — 설명" 한 가지 모양으로 맞추고, 순서도 나이 → 지역 → 신청 상태로 고정한다.
           대상 조건(eligibility) 도 '나이 · 지역 · 추가 조건' 같은 칸 순서를 쓴다. */
        List<String> reasons = new ArrayList<>();

        if (age != null) {
            boolean under34 = "youth-jeonse-02".equals(policy.getPolicyId())
                || "youth-savings-03".equals(policy.getPolicyId());
            reasons.add(String.format("만 %d세 — 대상 연령 만 19~%d세에 들어가요", age, under34 ? 34 : 39));
        }

        if ("youth-rent-01".equals(policy.getPolicyId())) {
            if (residenceRegion != null) {
                reasons.add(residenceRegion + " 거주 — 서울시 사업 대상이에요");
            }
        } else {
            reasons.add("전국 대상 — 거주지와 상관없이 신청할 수 있어요");
        }

        if ("신청 가능".equals(policy.getStatus())) {
            reasons.add("신청 가능 — 지금 접수 중이에요");
        } else {
            reasons.add("조건 확인 필요 — 신청 전에 세부 요건을 확인해주세요");
        }

        return PolicyItemDto.builder()
            .policyId(policy.getPolicyId())
            .name(policy.getName())
            .category(policy.getCategory())
            .description(policy.getDescription())
            .status(policy.getStatus())
            .link(policy.getLink())
            .supportAmount(policy.getSupportAmount())
            .supportNote(policy.getSupportNote())
            .eligibility(policy.getEligibility())
            .matchReasons(List.copyOf(reasons))
            .build();
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
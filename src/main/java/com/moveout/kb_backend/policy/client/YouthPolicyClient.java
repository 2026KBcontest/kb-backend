package com.moveout.kb_backend.policy.client;

import com.moveout.kb_backend.policy.dto.PolicyItemDto;
import com.moveout.kb_backend.policy.dto.PolicyRequest;
import com.moveout.kb_backend.policy.dto.PolicyResponse;
import com.moveout.kb_backend.policy.dto.YouthPolicyXmlResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
public class YouthPolicyClient {

    private final RestTemplate restTemplate;

    @Value("${youth.policy.api.key}")
    private String apiKey;

    @Value("${youth.policy.api.url:https://www.youthcenter.go.kr/opi/empSprtList.do}")
    private String apiUrl;

    public YouthPolicyClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 온통청년 API 호출 및 XML 파싱 (Spring Boot 3.x UriComponentsBuilder 규격)
     */
    public YouthPolicyXmlResponse fetchRawPolicyData(int pageIndex, int displayCount) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(apiUrl) // fromHttpUrl -> fromUriString 으로 변경
                    .queryParam("openApiVcntId", apiKey)
                    .queryParam("pageIndex", pageIndex)
                    .queryParam("display", displayCount)
                    .build()
                    .encode()
                    .toUri();

            return restTemplate.getForObject(uri, YouthPolicyXmlResponse.class);

        } catch (Exception e) {
            System.err.println("온통청년 API 호출 오류: " + e.getMessage());
            return null;
        }
    }

    /**
     * 컨트롤러 단 호환용 백업 API 호출 메서드
     */
    public PolicyResponse fetchPolicyData(PolicyRequest request) {
        YouthPolicyXmlResponse response = fetchRawPolicyData(1, 4);
        List<PolicyItemDto> items = new ArrayList<>();

        if (response != null && response.getEmpList() != null && !response.getEmpList().isEmpty()) {
            for (YouthPolicyXmlResponse.PolicyItem item : response.getEmpList()) {
                String rawSummary = item.getPolyItcnCn() != null ? item.getPolyItcnCn() : "청년 지원 정책입니다.";
                String desc = rawSummary.length() > 25 ? rawSummary.substring(0, 22) + "..." : rawSummary;

                items.add(PolicyItemDto.builder()
                        .policyId(item.getBizId() != null ? item.getBizId() : "policy-id")
                        .policyName(item.getPolyBizSjnm())
                        .description(desc)
                        .eligibility(item.getAgeInfo() != null ? item.getAgeInfo() : "만 19~34세 대상")
                        .status("신청 가능")
                        .link(item.getRqutUrla())
                        .build());
            }
            return new PolicyResponse(items, "온통청년 API 실시간 데이터 기반 추천 정책입니다.");
        }

        // Fallback 데이터
        String region = (request.getRegion() != null) ? request.getRegion() : "전국";
        items.add(PolicyItemDto.builder()
                .policyId("fallback-1")
                .policyName("[" + region + "] 청년 주거 지원 정책")
                .description("기본 주거 보증금 지원 안내")
                .eligibility("만 19세~34세 청년 대상")
                .status("조건 확인 필요")
                .link("https://www.youthcenter.go.kr")
                .build());

        return new PolicyResponse(items, "현재 API 응답이 원활하지 않아 기본 정책 목록을 안내합니다.");
    }
}
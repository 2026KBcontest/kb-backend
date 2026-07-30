package com.moveout.kb_backend.policy.client;

import com.moveout.kb_backend.policy.dto.PolicyRequest;
import com.moveout.kb_backend.policy.dto.PolicyResponse;
import com.moveout.kb_backend.policy.dto.YouthPolicyXmlResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

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
     * 온통청년 API 호출 및 XML 파싱
     */
    public YouthPolicyXmlResponse fetchRawPolicyData(int pageIndex, int displayCount) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(apiUrl)
                    .queryParam("openApiVcntId", apiKey)
                    .queryParam("pageIndex", pageIndex)
                    .queryParam("display", displayCount)
                    .build()
                    .encode()
                    .toUri();

            // XML 응답을 Java DTO 객체로 자동 파싱
            return restTemplate.getForObject(uri, YouthPolicyXmlResponse.class);

        } catch (Exception e) {
            System.err.println("온통청년 API 호출 오류: " + e.getMessage());
            return null;
        }
    }

    /**
     * 기존 컨트롤러 단과의 호환성을 위한 단건 호출 메서드
     */
    public PolicyResponse fetchPolicyData(PolicyRequest request) {
        YouthPolicyXmlResponse response = fetchRawPolicyData(1, 1);

        if (response != null && response.getEmpList() != null && !response.getEmpList().isEmpty()) {
            YouthPolicyXmlResponse.PolicyItem item = response.getEmpList().get(0);
            return new PolicyResponse(
                    item.getPolyBizSjnm(),
                    item.getPolyItcnCn(),
                    item.getAgeInfo(),
                    item.getRqutUrla()
            );
        }

        // Fallback 데이터
        String region = (request.getRegion() != null) ? request.getRegion() : "전국";
        return new PolicyResponse(
                "[" + region + "] 청년 주거 지원 정책",
                "현재 API 응답이 없거나 기본 안내 모드입니다.",
                "만 19세~34세 청년 대상",
                "https://www.youthcenter.go.kr"
        );
    }
}
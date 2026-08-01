package com.moveout.kb_backend.policy.client;

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
}
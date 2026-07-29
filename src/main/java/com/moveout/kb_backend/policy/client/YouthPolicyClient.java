package com.moveout.kb_backend.policy.client;

import com.moveout.kb_backend.policy.dto.PolicyRequest;
import com.moveout.kb_backend.policy.dto.PolicyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class YouthPolicyClient {

    private final RestTemplate restTemplate;

    // TODO: 공공데이터포털 또는 온통청년 API 인증키가 있다면 여기에 입력
    private final String API_KEY = "YOUR_API_KEY_HERE"; 

    public PolicyResponse fetchPolicyData(PolicyRequest request) {
        /* 
         * 실제 공공 API URL 호출 예시 로직
         * 인증키 발급 전이거나 외부 API 장애 시에도 안전하게 동작하도록 
         * 기본적인 추천 로직 및 fallback 응답 구조로 작성되었습니다.
         */
        
        // 예시: 사용자의 지역 조건(region)을 반영한 추천 응답 생성
        String region = (request.getRegion() != null) ? request.getRegion() : "전국";
        
        return new PolicyResponse(
                "[" + region + "] 청년 주거 보증금 대출 지원",
                region + " 거주 청년 대상 보증금 및 월세 이자 지원 정책",
                "만 19세~34세 / 소득 " + (request.getIncome() != null ? request.getIncome() : 0) + "만원 이하",
                "https://www.youthcenter.go.kr"
        );
    }
}
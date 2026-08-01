package com.moveout.kb_backend.ai.controller;

import com.moveout.kb_backend.ai.dto.AiAdviceRequest;
import com.moveout.kb_backend.ai.dto.AiAdviceResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/ai")
public class AiAdviceController {

    @PostMapping("/advice")
    public ResponseEntity<AiAdviceResponse> getAdvice(@RequestBody AiAdviceRequest request) {
        String defaultAnswer = "현재 자금 상황과 소득 조건을 고려할 때, 지원금 및 저축 플랜을 우선 활용하시는 것을 권장합니다. 부족한 금액에 대해서만 저리 대출 상품을 검토하시는 것이 안전합니다.";

        AiAdviceResponse response = AiAdviceResponse.builder()
                .source("ai")
                .text(defaultAnswer)
                .updatedAt(OffsetDateTime.now().toString())
                .build();

        return ResponseEntity.ok(response);
    }
}
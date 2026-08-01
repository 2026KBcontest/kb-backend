package com.moveout.kb_backend.ai.controller;

import com.moveout.kb_backend.ai.dto.AiAdviceRequest;
import com.moveout.kb_backend.ai.dto.AiAdviceResponse;
import com.moveout.kb_backend.ai.service.AiAdviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiAdviceController {

    private final AiAdviceService aiAdviceService;

    /**
     * AI 자취 코치 한마디.
     *
     * <p>저축 플랜·자금조달·정책 세 화면이 같은 엔드포인트를 쓴다. scope 로 구분한다.
     * 숫자는 화면이 계산해서 context 로 보내주므로 여기서 다시 계산하지 않는다.
     */
    @PostMapping("/advice")
    public ResponseEntity<AiAdviceResponse> getAdvice(@RequestBody AiAdviceRequest request) {
        return ResponseEntity.ok(aiAdviceService.advise(request));
    }
}

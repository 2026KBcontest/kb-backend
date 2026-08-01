package com.moveout.kb_backend.ai.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiAdviceResponse {
    private String source;    // "ai" 고정
    private String text;      // 3~4문장 답변
    private String updatedAt; // ISO-8601 시각
}
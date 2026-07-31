package com.moveout.kb_backend.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.Map;

@Getter
@NoArgsConstructor
public class AiAdviceRequest {
    private String scope;       // policy | saving | funding
    private String question;
    private Map<String, Object> context;
}
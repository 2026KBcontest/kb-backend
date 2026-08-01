package com.moveout.kb_backend.ai.client;

import java.util.List;

/**
 * Perplexity 가 돌려준 답변 한 건.
 *
 * <p>content 는 모델이 생성한 본문이다. JSON 스키마를 넘겼다면 JSON 문자열이 들어온다.
 * citations 는 모델이 참고한 웹 문서 URL 목록으로, 화면에 "출처" 로 붙일 수 있다.
 * (Perplexity 는 검색이 API 에 내장돼 있어서 추가 비용 없이 출처가 따라온다)
 */
public record AiAnswer(String content, List<String> citations) {}

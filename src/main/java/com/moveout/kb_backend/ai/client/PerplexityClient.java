package com.moveout.kb_backend.ai.client;

import jakarta.annotation.PostConstruct;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

/**
 * Perplexity(Sonar) 호출 담당.
 *
 * <p>이 프로젝트에서 Perplexity 와 직접 통신하는 곳은 여기 하나뿐이다.
 * 정책 추천이든 AI 어드바이스든 {@code ask(...)} 한 줄만 부르면 된다.
 *
 * <h3>이 클래스가 지키는 규칙</h3>
 * <ol>
 *   <li><b>절대 예외를 위로 던지지 않는다.</b> 실패하면 null 을 돌려준다.
 *       호출한 쪽은 null 을 보고 규칙 기반 추천으로 넘어가면 되고,
 *       그래서 시연 중 API 가 먹통이어도 화면은 멀쩡하다.</li>
 *   <li><b>프롬프트는 만들지 않는다.</b> 무엇을 물어볼지는 각 서비스가 정한다.
 *       여기는 전달과 파싱만 한다. 역할이 섞이면 나중에 고치기 어렵다.</li>
 *   <li><b>캐시와 하루 상한을 반드시 통과시킨다.</b> 크레딧이 갑자기 사라지는 일을
 *       코드 구조로 막는다.</li>
 * </ol>
 *
 * <h3>비용</h3>
 * Sonar + low context 기준 1회 약 10원. $10 크레딧이면 약 1,400회다.
 * search_context_size 를 올리면 요청 수수료가 2배 이상 뛰므로 low 로 고정한다.
 * 우리는 정책·상품 목록을 프롬프트에 직접 넣어주기 때문에 웹 검색에 기댈 이유가 없다.
 *
 * <h3>설정</h3>
 * <pre>
 * perplexity.enabled      개발 중에는 false. 진짜 호출은 검증·리허설·시연 때만.
 * perplexity.api.key      환경변수 PERPLEXITY_API_KEY 로 주입 (코드에 적지 말 것)
 * perplexity.daily-limit  하루 호출 상한 (기본 300)
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PerplexityClient {

    private final ObjectMapper objectMapper;
    private final AiCache cache;
    private final AiBudgetGuard budgetGuard;

    @Value("${perplexity.enabled:false}")
    private boolean enabled;

    @Value("${perplexity.api.key:}")
    private String apiKey;

    @Value("${perplexity.api.url:https://api.perplexity.ai/chat/completions}")
    private String apiUrl;

    @Value("${perplexity.model:sonar}")
    private String model;

    @Value("${perplexity.timeout-seconds:20}")
    private int timeoutSeconds;

    /** 답이 매번 달라지면 시연이 불안하다. 낮게 고정한다. */
    @Value("${perplexity.temperature:0.2}")
    private double temperature;

    @Value("${perplexity.max-tokens:700}")
    private int maxTokens;

    private RestClient restClient;

    @PostConstruct
    void init() {
        HttpClient httpClient =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder().requestFactory(factory).build();

        if (!enabled) {
            log.info("Perplexity 연동이 꺼져 있습니다(perplexity.enabled=false). 규칙 기반으로 동작합니다.");
        } else if (apiKey == null || apiKey.isBlank()) {
            log.warn("PERPLEXITY_API_KEY 가 비어 있습니다. 규칙 기반으로 동작합니다.");
        }
    }

    /**
     * 프롬프트를 보내고 답변을 받는다.
     *
     * @param systemPrompt 모델이 지켜야 할 규칙 (예: "목록 밖은 말하지 마세요")
     * @param userPrompt 실제 질문 + 근거 데이터
     * @param jsonSchema 정해진 JSON 모양으로만 답하게 강제할 스키마. 자유 문장이면 null.
     * @return 답변. <b>실패하면 null</b> — 호출한 쪽에서 규칙 기반으로 넘어갈 것.
     */
    public AiAnswer ask(String systemPrompt, String userPrompt, Map<String, Object> jsonSchema) {
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return null; // 아직 안 붙였거나 키가 없음 → 조용히 규칙 기반으로
        }

        // ① 캐시 — 같은 프롬프트면 호출하지 않는다 (과금 0원)
        String key = cacheKey(systemPrompt, userPrompt, jsonSchema);
        AiAnswer cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        // ② 하루 상한 — 반복 호출 사고를 여기서 끊는다
        if (!budgetGuard.allow()) {
            return null;
        }

        try {
            AiAnswer answer = call(systemPrompt, userPrompt, jsonSchema);
            if (answer != null) {
                cache.put(key, answer);
            }
            return answer;
        } catch (Exception e) {
            // ★ 위로 던지지 않는다. AI 가 죽어도 서비스는 살아야 한다.
            log.warn("Perplexity 호출 실패 — 규칙 기반으로 대체합니다: {}", e.getMessage());
            return null;
        }
    }

    /** 자유 문장 답변이 필요할 때 (JSON 스키마 없이). */
    public AiAnswer ask(String systemPrompt, String userPrompt) {
        return ask(systemPrompt, userPrompt, null);
    }

    /* ---------- 내부 ---------- */

    private AiAnswer call(String systemPrompt, String userPrompt, Map<String, Object> jsonSchema) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put(
                "messages",
                List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);

        // 검색량을 최소로 — 요청 수수료가 여기서 갈린다 (low $5 / high $12 per 1,000건)
        body.put("web_search_options", Map.of("search_context_size", "low"));

        // 구조화 출력 : 정해진 JSON 모양으로만 답하게 강제한다.
        // 새 스키마의 첫 호출은 준비에 10~30초 걸린다. 시연 전 한 번 예열해둘 것.
        if (jsonSchema != null) {
            body.put(
                    "response_format",
                    Map.of(
                            "type",
                            "json_schema",
                            "json_schema",
                            Map.of("name", "kb_advice", "schema", jsonSchema)));
        }

        String raw =
                restClient
                        .post()
                        .uri(apiUrl)
                        .header("Authorization", "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(String.class);

        return parse(raw);
    }

    /**
     * 응답 구조는 OpenAI 와 같다. choices[0].message.content 가 본문이다.
     *
     * <p>JsonNode 대신 Map 으로 읽는다. Jackson 버전에 따라 JsonNode 메서드 이름이
     * 달라진 적이 있어서, 어디서든 같게 동작하는 쪽을 택했다.
     */
    @SuppressWarnings("unchecked")
    private AiAnswer parse(String raw) {
        if (raw == null || raw.isBlank()) {
            log.warn("Perplexity 응답이 비어 있습니다.");
            return null;
        }
        try {
            Map<String, Object> root = objectMapper.readValue(raw, Map.class);

            Object choicesValue = root.get("choices");
            if (!(choicesValue instanceof List<?> choices) || choices.isEmpty()) {
                log.warn("Perplexity 응답에 choices 가 없습니다: {}", shorten(raw));
                return null;
            }

            Map<String, Object> first = (Map<String, Object>) choices.get(0);
            Object messageValue = first.get("message");
            if (!(messageValue instanceof Map<?, ?> message)) {
                return null;
            }

            Object contentValue = message.get("content");
            if (!(contentValue instanceof String content) || content.isBlank()) {
                return null;
            }

            // 검색이 API 에 내장돼 있어 참고한 문서 URL 이 따라온다. 화면에 출처로 붙일 수 있다.
            List<String> citations = new ArrayList<>();
            if (root.get("citations") instanceof List<?> list) {
                for (Object item : list) {
                    if (item != null) {
                        citations.add(item.toString());
                    }
                }
            }

            return new AiAnswer(content, List.copyOf(citations));
        } catch (Exception e) {
            log.warn("Perplexity 응답 파싱 실패: {}", shorten(raw));
            return null;
        }
    }

    /** 프롬프트가 같으면 같은 키. 조건이 바뀌면 키가 달라져 자동으로 새로 호출된다. */
    private String cacheKey(String systemPrompt, String userPrompt, Map<String, Object> jsonSchema) {
        String source = model + "|" + systemPrompt + "|" + userPrompt + "|" + jsonSchema;
        try {
            byte[] hash =
                    MessageDigest.getInstance("SHA-256")
                            .digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 은 모든 JDK 에 있으므로 사실상 오지 않는다. 와도 캐시만 못 쓸 뿐이다.
            return source;
        }
    }

    /** 로그에 응답 전체를 남기면 읽기 어렵다. 앞부분만 남긴다. */
    private String shorten(String text) {
        return text.length() <= 300 ? text : text.substring(0, 300) + "...";
    }
}

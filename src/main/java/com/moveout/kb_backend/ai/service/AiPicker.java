package com.moveout.kb_backend.ai.service;

import com.moveout.kb_backend.ai.client.AiAnswer;
import com.moveout.kb_backend.ai.client.PerplexityClient;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * "여러 후보 중 하나를 고르고 이유를 쓰는" 일을 AI 에게 맡기는 공통 창구.
 *
 * <p>정책 추천, 지역 변경 추천, 저축 상품 추천이 전부 같은 모양의 일이다.
 * 후보 목록을 주고 → 하나 고르게 하고 → 고른 게 진짜 목록에 있는지 확인한다.
 * 세 군데에 같은 코드를 쓰지 않으려고 여기에 모았다.
 *
 * <h3>지켜야 할 두 가지</h3>
 *
 * <ol>
 *   <li><b>숫자는 넘겨주기만 한다.</b> 금액·기간은 서버가 계산해서 프롬프트에 적어 보내고,
 *       AI 에게는 "적힌 값을 그대로 쓰라" 고 못 박는다. LLM 은 산수를 틀린다.
 *   <li><b>목록 밖 답은 통째로 버린다.</b> AI 가 없는 후보를 지어내면 그 응답을 쓰지 않고
 *       null 을 돌려준다. 부르는 쪽은 규칙 기반으로 넘어가면 되므로 화면은 멀쩡하다.
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiPicker {

    private static final String SYSTEM =
            """
            당신은 한국 청년의 주거·금융을 돕는 상담사입니다.
            아래 규칙을 반드시 지키세요.

            1. 반드시 [후보] 안에서만 고르세요. 목록에 없는 것을 만들어내지 마세요.
            2. 금액·기간·비율은 [후보]와 [사용자]에 적힌 값을 그대로 쓰세요.
               직접 계산하거나 어림잡지 마세요.
            3. 존댓말로 쓰고, 한 문장은 40자를 넘기지 마세요.
            4. 확실하지 않으면 단정하지 말고 "확인이 필요해요" 라고 쓰세요.
            5. 사용자를 탓하거나 다그치는 표현을 쓰지 마세요.
            6. 고르는 사람은 사용자입니다. 당신은 후보 하나를 권해보는 역할입니다.
               headline 은 지시가 아니라 제안으로 쓰세요.
                 쓰지 말 것 : "~하세요", "~하시면 됩니다", "~해야 해요", "~을 추천합니다"
                 이렇게 쓸 것 : "~는 어떠세요?", "~도 살펴볼 만해요", "~를 눈여겨볼 만해요"
            7. reasons 에는 왜 그렇게 봤는지를 적으세요. 지시문을 반복하지 마세요.
            """;

    /** 답변을 이 모양으로만 하게 강제한다. 자유 문장으로 받으면 화면에 넣을 수 없다. */
    private static final Map<String, Object> SCHEMA =
            Map.of(
                    "type",
                    "object",
                    "properties",
                    Map.of(
                            "pickId", Map.of("type", "string"),
                            "headline", Map.of("type", "string"),
                            "reasons",
                                    Map.of(
                                            "type", "array",
                                            "items", Map.of("type", "string")),
                            "altId", Map.of("type", "string")),
                    "required",
                    List.of("pickId", "headline", "reasons"));

    private final PerplexityClient perplexityClient;
    private final ObjectMapper objectMapper;

    /**
     * 고른 결과.
     *
     * @param pickId 고른 후보의 id. 반드시 넘겨준 후보 안의 값
     * @param altId 그다음으로 볼 만한 후보. 없으면 null
     * @param citations AI 가 참고한 웹 문서 (Perplexity 는 검색이 내장돼 있다)
     */
    public record Pick(
            String pickId,
            String headline,
            List<String> reasons,
            String altId,
            List<String> citations) {}

    /**
     * 후보 중 하나를 고르게 한다.
     *
     * @param userBlock 사용자 상황을 적은 여러 줄 문자열 (나이·소득·목표 등)
     * @param optionLines 후보 한 줄씩. 반드시 {@code id=xxx | ...} 형태로 시작할 것
     * @param validIds 유효한 id 목록. AI 응답 검증에 쓴다
     * @param instruction 무엇을 골라달라는 한 줄 지시
     * @return 고른 결과. <b>실패하거나 목록 밖을 고르면 null</b>
     */
    public Pick pick(
            String userBlock,
            List<String> optionLines,
            Collection<String> validIds,
            String instruction) {

        if (optionLines == null || optionLines.isEmpty()) {
            return null; // 고를 게 없으면 물어볼 이유도 없다 (호출 = 비용)
        }

        String userPrompt =
                """
                [사용자]
                %s

                [후보]
                %s

                %s
                pickId 에는 후보 줄 맨 앞의 id 값을 그대로 적으세요.
                reasons 는 2~3개로 적으세요.
                """
                        .formatted(userBlock, String.join("\n", optionLines), instruction);

        AiAnswer answer = perplexityClient.ask(SYSTEM, userPrompt, SCHEMA);
        if (answer == null) {
            return null; // 꺼져 있거나 호출 실패 → 규칙 기반으로
        }
        return parseAndVerify(answer, Set.copyOf(validIds));
    }

    @SuppressWarnings("unchecked")
    private Pick parseAndVerify(AiAnswer answer, Set<String> validIds) {
        try {
            Map<String, Object> root = objectMapper.readValue(answer.content(), Map.class);

            String pickId = asString(root.get("pickId"));
            String headline = asString(root.get("headline"));

            // ★ 환각 차단 — 목록에 없는 걸 골랐으면 응답 전체를 버린다.
            //    일부만 고쳐서 쓰면 이유 문장이 엉뚱한 후보를 설명하게 된다.
            if (pickId == null || !validIds.contains(pickId)) {
                log.warn("AI 가 목록에 없는 후보를 골랐습니다: {} (유효: {})", pickId, validIds);
                return null;
            }
            if (headline == null || headline.isBlank()) {
                return null;
            }

            List<String> reasons = new ArrayList<>();
            if (root.get("reasons") instanceof List<?> list) {
                for (Object item : list) {
                    String text = asString(item);
                    if (text != null && !text.isBlank()) {
                        reasons.add(text);
                    }
                }
            }
            if (reasons.isEmpty()) {
                return null; // 이유 없는 추천은 카드로 보여줄 수 없다
            }

            String altId = asString(root.get("altId"));
            if (altId != null && (!validIds.contains(altId) || altId.equals(pickId))) {
                altId = null; // 대안만 이상하면 그것만 버린다
            }

            return new Pick(pickId, headline, List.copyOf(reasons), altId, answer.citations());
        } catch (Exception e) {
            log.warn("AI 응답을 해석하지 못했습니다: {}", e.getMessage());
            return null;
        }
    }

    private String asString(Object value) {
        return value instanceof String s ? s : null;
    }
}

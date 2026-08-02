package com.moveout.kb_backend.ai.service;

import com.moveout.kb_backend.ai.client.AiAnswer;
import com.moveout.kb_backend.ai.client.CitationMarks;
import com.moveout.kb_backend.ai.client.PerplexityClient;
import com.moveout.kb_backend.ai.dto.AiAdviceRequest;
import com.moveout.kb_backend.ai.dto.AiAdviceResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * AI 자취 코치 한마디.
 *
 * <p>화면이 이미 계산해둔 숫자(context)를 그대로 프롬프트에 넣고, "이 상황에서 무엇을 먼저
 * 하면 좋을지" 만 묻는다. <b>서버도 AI 도 숫자를 다시 계산하지 않는다.</b>
 * 같은 화면에 보이는 값과 답변의 숫자가 어긋나면 그 순간 신뢰를 잃기 때문이다.
 *
 * <p>AI 가 꺼져 있거나 실패하면 source 를 "rule" 로 두고 미리 준비한 문장을 돌려준다.
 * 화면에는 'AI 분석' 대신 '조건 기반' 배지가 붙어, 사용자가 오해하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class AiAdviceService {

    private static final String SYSTEM =
            """
            당신은 한국 청년의 주거 자금을 돕는 상담사입니다.
            아래 규칙을 반드시 지키세요.

            1. [상황]에 적힌 숫자만 사용하세요. 직접 계산하거나 새로운 숫자를 만들지 마세요.
            2. 3~4문장으로 짧게 답하세요. 한 문장은 40자를 넘기지 마세요.
            3. 무엇을 먼저 하면 좋을지 한 가지를 분명히 알려주세요.
            4. 존댓말로 쓰고, 사용자를 탓하거나 다그치지 마세요.
            5. 확실하지 않으면 단정하지 말고 "확인이 필요해요" 라고 쓰세요.
            6. [1], [2][5] 같은 출처 번호를 문장에 넣지 마세요. 화면에 그대로 노출됩니다.
            """;

    /** scope 별로 무엇을 봐야 하는지 알려준다. 같은 숫자라도 화면마다 관심사가 다르다. */
    private static final Map<String, String> SCOPE_FOCUS =
            Map.of(
                    "policy", "받을 수 있는 지원금을 먼저 챙기는 관점에서 답해주세요.",
                    "saving", "저축 목표가 현실적인지, 목표 시점을 앞당길 방법이 있는지 관점에서 답해주세요.",
                    "funding", "대출을 늘리는 것과 더 모으는 것 중 무엇이 나은지 관점에서 답해주세요.",
                    "region", "지역을 바꾸는 것이 도움이 되는지 관점에서 답해주세요.");

    private static final String FALLBACK_TEXT =
            "지원금을 먼저 확인하고, 부족한 금액만 대출로 채우는 편이 안전해요. "
                    + "월 저축 목표를 정해두면 자취 시점을 더 정확히 볼 수 있어요.";

    private final PerplexityClient perplexityClient;

    public AiAdviceResponse advise(AiAdviceRequest request) {
        AiAnswer answer =
                perplexityClient.ask(SYSTEM, buildPrompt(request)); // 자유 문장이라 스키마 없음

        boolean answered = answer != null && !answer.content().isBlank();

        return AiAdviceResponse.builder()
                // 진짜 AI 답변일 때만 "ai". 준비된 문장을 돌려줄 땐 "rule" 로 정직하게 표기한다.
                .source(answered ? "ai" : "rule")
                // 본문에 박혀 오는 출처 번호([1][6])를 걷어낸다. 화면에 그 번호가 가리킬 목록이 없다.
                .text(answered ? CitationMarks.strip(answer.content()) : FALLBACK_TEXT)
                .updatedAt(OffsetDateTime.now().toString())
                .build();
    }

    private String buildPrompt(AiAdviceRequest request) {
        String focus =
                SCOPE_FOCUS.getOrDefault(
                        request.getScope() == null ? "" : request.getScope(),
                        "이 사용자에게 지금 무엇이 가장 도움이 될지 답해주세요.");

        String question =
                (request.getQuestion() == null || request.getQuestion().isBlank())
                        ? "지금 상황에서 무엇을 먼저 하면 좋을까요?"
                        : request.getQuestion();

        return """
               [상황]
               %s

               [질문]
               %s

               %s
               """
                .formatted(formatContext(request.getContext()), question, focus);
    }

    /** context 를 "키 : 값" 여러 줄로 편다. 값은 서버가 준 그대로 쓴다. */
    private String formatContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return "제공된 수치가 없습니다.";
        }
        List<String> lines =
                context.entrySet().stream()
                        .map(entry -> label(entry.getKey()) + " : " + format(entry.getValue()))
                        .toList();
        return String.join("\n", lines);
    }

    /** 프론트가 보내는 키를 사람이 읽는 말로 바꾼다. 영어 키를 그대로 두면 답변이 어색해진다. */
    private String label(String key) {
        return switch (key) {
            case "required" -> "필요한 초기 자금";
            case "own" -> "지금 모은 돈";
            case "support" -> "받을 수 있는 지원금";
            case "loan" -> "대출로 채울 금액";
            case "dsr" -> "DSR(소득 대비 상환 부담, %)";
            case "monthlyPayment" -> "매달 갚아야 할 금액";
            case "capacity" -> "매달 모을 수 있는 최대 금액";
            case "goal" -> "사용자가 정한 월 저축 목표";
            case "baseMonths" -> "지금 속도로 자취까지 걸리는 개월";
            case "goalMonths" -> "목표대로 모을 때 걸리는 개월";
            case "remaining" -> "아직 더 모아야 하는 금액";
            default -> key;
        };
    }

    private String format(Object value) {
        if (value instanceof Number number) {
            // 소수점이 있는 값(DSR 등)은 그대로, 정수는 천 단위 구분해서 읽기 좋게
            double d = number.doubleValue();
            return d == Math.floor(d) ? String.format("%,d", (long) d) : String.valueOf(d);
        }
        return String.valueOf(value);
    }
}

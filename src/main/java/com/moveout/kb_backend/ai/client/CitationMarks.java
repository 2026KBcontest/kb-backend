package com.moveout.kb_backend.ai.client;

import java.util.regex.Pattern;

/**
 * AI 답변에 섞여 오는 출처 표시({@code [1]}, {@code [2][5]})를 걷어낸다.
 *
 * <p>Perplexity 는 검색이 내장돼 있어서 문장 끝마다 참고한 문서 번호를 대괄호로 붙인다.
 * 그런데 우리 화면에는 그 번호가 가리키는 목록이 없다. 사용자에게는 <b>뜻을 알 수 없는
 * 기호</b>로만 보이고, 카드 문장이 잘리거나 어색해진다.
 *
 * <p>프롬프트로 "출처 표시를 쓰지 마세요" 라고 일러도 모델은 종종 붙인다.
 * 화면에 나가는 글이라 지시에만 맡기지 않고 여기서 한 번 더 거른다.
 *
 * <p><b>URL 목록 자체는 버리지 않는다</b> — {@link AiAnswer#citations()} 에 그대로 남아 있어서
 * 나중에 "출처" 를 화면에 붙이기로 하면 그때 쓰면 된다. 지우는 건 본문에 박힌 번호뿐이다.
 */
public final class CitationMarks {

    /** {@code [1]}, {@code [12]}, {@code [1,2]}, {@code [1, 2]} 를 모두 잡는다. */
    private static final Pattern MARKS = Pattern.compile("\\[\\d+(?:\\s*,\\s*\\d+)*\\]");

    /** 마커가 빠진 자리에 남는 이중 공백. */
    private static final Pattern DOUBLE_SPACE = Pattern.compile("[ \\t]{2,}");

    /** "확인이 필요해요 ." 처럼 구두점 앞에 남는 공백. 줄바꿈은 건드리지 않는다. */
    private static final Pattern SPACE_BEFORE_PUNCT = Pattern.compile("[ \\t]+([.,!?~)\\]])");

    private CitationMarks() {}

    /**
     * 본문에서 출처 번호를 지운다.
     *
     * @param text 원본. null 이나 빈 문자열이면 그대로 돌려준다
     * @return 번호를 지우고 공백을 정리한 문장
     */
    public static String strip(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String cleaned = MARKS.matcher(text).replaceAll("");
        cleaned = DOUBLE_SPACE.matcher(cleaned).replaceAll(" ");
        cleaned = SPACE_BEFORE_PUNCT.matcher(cleaned).replaceAll("$1");
        return cleaned.trim();
    }
}

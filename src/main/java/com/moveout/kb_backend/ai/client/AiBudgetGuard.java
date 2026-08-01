package com.moveout.kb_backend.ai.client;

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI 호출 하루 상한.
 *
 * <p>크레딧을 날리는 원인은 비싼 요금이 아니라 잘못 짠 반복 호출이다.
 * 프론트의 useEffect 의존성 실수 하나로 초당 수십 번이 나가면
 * 몇 분 만에 크레딧이 사라진다. 그런 일이 구조적으로 불가능하게 막는다.
 *
 * <p>상한에 걸리면 예외를 던지지 않고 false 를 돌려준다.
 * 호출한 쪽은 규칙 기반 추천으로 넘어가므로 화면은 그대로 동작한다.
 */
@Slf4j
@Component
public class AiBudgetGuard {

    /** 하루 최대 호출 수. Sonar 기준 300회면 약 $2 이다. */
    @Value("${perplexity.daily-limit:300}")
    private int dailyLimit;

    private LocalDate day = LocalDate.now();
    private int count = 0;

    /** 호출해도 되면 true 를 돌려주고 카운트를 1 올린다. */
    public synchronized boolean allow() {
        LocalDate today = LocalDate.now();
        if (!day.equals(today)) { // 날짜가 바뀌면 카운터를 0 으로
            day = today;
            count = 0;
        }
        if (count >= dailyLimit) {
            log.warn("AI 호출 하루 상한({}회)에 도달했습니다. 규칙 기반으로 대체합니다.", dailyLimit);
            return false;
        }
        count++;
        return true;
    }

    /** 오늘 몇 번 썼는지 (로그·확인용). */
    public synchronized int usedToday() {
        return day.equals(LocalDate.now()) ? count : 0;
    }
}

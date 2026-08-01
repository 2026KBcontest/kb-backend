package com.moveout.kb_backend.ai.client;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * AI 응답 캐시.
 *
 * <p>같은 질문에는 같은 답이 나온다. 그런데 화면을 열 때마다 API 를 부르면
 * 사용자가 정책 화면을 열 번 들락거릴 때 열 번 과금된다.
 * 프롬프트가 같으면 저장해둔 답을 그대로 돌려줘서 호출 자체를 없앤다.
 *
 * <p>Redis 를 붙일 만한 규모가 아니라 메모리 맵으로 둔다. 서버를 재시작하면 비워진다.
 * 시연 중에는 두 번째 조회부터 즉시 응답이라 화면도 빨라진다.
 */
@Component
public class AiCache {

    /** 정책·상품 정보가 하루 사이에 바뀌지는 않으므로 6시간이면 충분하다. */
    private static final Duration TTL = Duration.ofHours(6);

    /** 메모리가 무한정 늘지 않게 상한을 둔다. 넘으면 통째로 비운다(단순하고 안전한 방법). */
    private static final int MAX_ENTRIES = 500;

    private record Entry(AiAnswer value, long expireAt) {}

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    /** 유효한 캐시가 있으면 반환, 없거나 만료됐으면 null. */
    public AiAnswer get(String key) {
        Entry entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expireAt() < System.currentTimeMillis()) {
            store.remove(key);
            return null;
        }
        return entry.value();
    }

    public void put(String key, AiAnswer value) {
        if (store.size() >= MAX_ENTRIES) {
            store.clear();
        }
        store.put(key, new Entry(value, System.currentTimeMillis() + TTL.toMillis()));
    }

    /** 테스트나 운영 중 강제 갱신용. */
    public void clear() {
        store.clear();
    }
}

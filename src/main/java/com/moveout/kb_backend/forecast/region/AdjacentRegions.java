package com.moveout.kb_backend.forecast.region;

import java.util.List;
import java.util.Map;

/**
 * 서울 25개 자치구의 인접 관계.
 *
 * <p><b>왜 AI 에게 물어보지 않고 표로 두는가</b><br>
 * 어느 구가 어느 구와 붙어 있는지는 변하지 않는 사실이다. 변하지 않는 사실을
 * 매번 LLM 에게 묻는 것은 느리고, 비용이 들고, 가끔 틀린 답이 온다.
 * AI 는 "어디로 옮기라고 권할지 고르고 설명하는" 일에 쓴다. (RegionSwitchService 참고)
 *
 * <p>생활권이 크게 바뀌지 않는 범위를 보여주는 것이 목적이라 행정 경계 기준으로 적었다.
 * 한강을 사이에 둔 구는 다리로 이어져 실제 생활권이 이어지는 경우만 포함했다.
 */
public final class AdjacentRegions {

    private static final Map<String, List<String>> MAP = Map.ofEntries(
            Map.entry("종로구", List.of("중구", "서대문구", "은평구", "성북구", "동대문구", "용산구", "강북구")),
            Map.entry("중구", List.of("종로구", "용산구", "성동구", "서대문구", "동대문구")),
            Map.entry("용산구", List.of("중구", "종로구", "서대문구", "마포구", "성동구", "동작구", "영등포구")),
            Map.entry("성동구", List.of("중구", "용산구", "동대문구", "광진구", "강남구")),
            Map.entry("광진구", List.of("성동구", "동대문구", "중랑구", "강남구", "송파구", "강동구")),
            Map.entry("동대문구", List.of("종로구", "중구", "성동구", "광진구", "중랑구", "성북구")),
            Map.entry("중랑구", List.of("동대문구", "성북구", "노원구", "광진구")),
            Map.entry("성북구", List.of("종로구", "동대문구", "중랑구", "노원구", "강북구")),
            Map.entry("강북구", List.of("성북구", "도봉구", "노원구", "종로구")),
            Map.entry("도봉구", List.of("강북구", "노원구")),
            Map.entry("노원구", List.of("도봉구", "강북구", "성북구", "중랑구")),
            Map.entry("은평구", List.of("종로구", "서대문구", "마포구")),
            Map.entry("서대문구", List.of("종로구", "중구", "은평구", "마포구", "용산구")),
            Map.entry("마포구", List.of("서대문구", "은평구", "용산구", "영등포구", "강서구")),
            Map.entry("양천구", List.of("강서구", "구로구", "영등포구")),
            Map.entry("강서구", List.of("양천구", "구로구", "마포구", "영등포구")),
            Map.entry("구로구", List.of("양천구", "강서구", "영등포구", "금천구", "관악구", "동작구")),
            Map.entry("금천구", List.of("구로구", "관악구", "영등포구")),
            Map.entry("영등포구", List.of("마포구", "강서구", "양천구", "구로구", "동작구", "용산구", "금천구")),
            Map.entry("동작구", List.of("영등포구", "관악구", "서초구", "용산구", "구로구")),
            Map.entry("관악구", List.of("동작구", "서초구", "금천구", "구로구")),
            Map.entry("서초구", List.of("관악구", "동작구", "강남구")),
            Map.entry("강남구", List.of("서초구", "송파구", "성동구", "광진구")),
            Map.entry("송파구", List.of("강남구", "강동구", "광진구")),
            Map.entry("강동구", List.of("송파구", "광진구")));

    private AdjacentRegions() {}

    /** 붙어 있는 구 목록. 모르는 지역이면 빈 목록. */
    public static List<String> of(String region) {
        return MAP.getOrDefault(region, List.of());
    }

    public static boolean contains(String region) {
        return MAP.containsKey(region);
    }
}

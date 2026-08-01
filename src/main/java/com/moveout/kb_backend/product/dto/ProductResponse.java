package com.moveout.kb_backend.product.dto;

import java.util.List;

/**
 * 금융상품 목록 응답.
 *
 * @param products 상품 목록
 * @param recommendation AI 추천. 못 만들면 null 이고, 화면은 목록만으로도 동작한다.
 */
public record ProductResponse(List<Item> products, Recommendation recommendation) {

    /**
     * @param category LOAN | SAVINGS — 화면이 이 값으로 자금조달/저축 화면을 나눈다
     * @param tag 카드 오른쪽 위 배지 (예: "최저 금리")
     * @param target 신청 대상 조건. 정책 카드와 같은 '나이 · 지역 · 추가 조건' 순서
     * @param specs 카드에 그대로 찍는 표시용 문자열
     * @param calc 화면이 계산에 쓰는 원시 숫자
     */
    public record Item(
            String productId,
            String name,
            String category,
            String tag,
            String target,
            List<Spec> specs,
            Calc calc,
            String link) {}

    public record Spec(String label, String value) {}

    /**
     * @param assumedYears 상환 기간 가정값.
     *     <p>원본 데이터에 상환 기간이 없어서 DSR 을 계산하려면 기간을 정해야 한다.
     *     지어낸 값을 상품 정보인 척 내보내지 않으려고 이름에 'assumed' 를 박아두고,
     *     화면에도 "10년 가정" 이라고 표시한다.
     */
    public record Calc(Long maxLimit, Double minRate, Double maxRate, Integer assumedYears) {}

    /**
     * @param source "ai" 면 AI 가 고른 것, "rule" 이면 금리 기준으로 고른 것
     * @param pickId 추천 상품의 productId. 반드시 products 안에 있는 값
     */
    public record Recommendation(
            String source, String pickId, String headline, List<String> reasons) {}
}

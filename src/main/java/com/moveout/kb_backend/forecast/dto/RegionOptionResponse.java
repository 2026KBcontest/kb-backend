package com.moveout.kb_backend.forecast.dto;

import com.moveout.kb_backend.forecast.entity.HousingType;
import java.util.List;

/**
 * 지역 변경 추천 응답.
 *
 * <p>숫자는 전부 서버가 계산한 값이다. AI 는 candidates 중에서 무엇을 권할지 고르고
 * 이유를 쓰는 일만 한다 (recommendation). AI 가 아직 안 붙었으면 recommendation 은 null 이고,
 * 화면은 candidates 만으로도 정상 동작한다.
 *
 * @param current 지금 목표로 잡은 지역
 * @param candidates 인접 구 중 지금보다 싼 곳 (절감액 큰 순)
 * @param recommendation AI 추천. 없으면 null
 */
public record RegionOptionResponse(
        Current current, List<Candidate> candidates, Recommendation recommendation) {

    /**
     * @param estimatedMonths 자취까지 남은 개월. 이미 모았으면 0, 저축 여력이 없으면 null
     */
    public record Current(
            String region,
            HousingType housingType,
            Long requiredAmount,
            Integer estimatedMonths) {}

    /**
     * @param savedAmount 초기 자금이 덜 드는 금액 (원). <b>전세에서 의미 있는 값</b>
     * @param monthlyRentSaved 매달 월세가 덜 나가는 금액 (원). <b>월세에서 의미 있는 값</b>
     * @param shortenMonths 지금보다 몇 개월 빨라지는지. 계산 불가면 null
     *     <p>전세와 월세는 아껴지는 자리가 다르다. 전세는 보증금이 통째로 줄어 초기 자금이
     *     크게 빠지고, 월세는 보증금이 고정이라 초기 차이는 작은 대신 매달 나가는 돈이 준다.
     *     둘을 한 숫자로 합치면 월세 추천이 "옮길 이유 없음" 처럼 보인다(실제로 그랬다).
     */
    public record Candidate(
            String region,
            Long deposit,
            Long monthlyRent,
            Long requiredAmount,
            Long savedAmount,
            Long monthlyRentSaved,
            Integer estimatedMonths,
            Integer shortenMonths) {}

    /**
     * @param source "ai" 면 AI 가 고른 것, "rule" 이면 절감액 기준으로 고른 것
     * @param pickRegion 권하는 지역 이름. 반드시 candidates 안에 있는 값
     */
    public record Recommendation(
            String source, String pickRegion, String headline, List<String> reasons) {}
}

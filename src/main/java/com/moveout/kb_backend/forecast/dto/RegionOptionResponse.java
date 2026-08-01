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
 * @param recommendation 대표 추천 한 개. picks 의 첫 칸과 같은 지역이다
 * @param picks 성격이 다른 추천 묶음 (아래 Pick 참고). 후보가 없으면 빈 목록
 */
public record RegionOptionResponse(
        Current current,
        List<Candidate> candidates,
        Recommendation recommendation,
        List<Pick> picks) {

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

    /**
     * 성격이 다른 추천 한 칸.
     *
     * <p><b>왜 하나가 아니라 여러 개인가</b> — "가장 싼 곳" 하나만 내놓으면 사용자는 그게
     * 유일한 답이라고 읽는다. 그런데 사는 곳은 돈만으로 정하지 않는다. 성격이 다른 선택지를
     * 나란히 놓아야 무엇을 포기하고 무엇을 얻는지가 보이고, 고르는 일이 사용자에게 남는다.
     *
     * <p><b>어디까지 서버가 계산했는지 kind 로 구분한다.</b>
     *
     * <ul>
     *   <li>{@code cheapest} · {@code fastest} — 서버 계산. 금액과 개월 수로 정해지므로 틀릴 일이 없다
     *   <li>{@code similar} — AI 판단. 지하철·도심 접근성·상권 같은 건 우리 데이터에 없다
     * </ul>
     *
     * 화면은 {@code source} 가 "ai" 인 칸에만 AI 배지를 붙인다. 계산으로 고른 걸
     * AI 가 고른 것처럼 보이게 하지 않기 위해서다.
     *
     * @param kind "cheapest" | "similar" | "fastest"
     * @param label 화면에 그대로 쓰는 칸 이름 (예: "가장 저렴한 곳")
     * @param source "ai" | "rule"
     * @param region 고른 지역. 반드시 candidates 안에 있는 값
     * @param headline 한 줄 요약. 지시조가 아니라 제안조로 쓴다
     * @param reasons 왜 이 칸에 이 지역인지 (2~3개)
     */
    public record Pick(
            String kind,
            String label,
            String source,
            String region,
            String headline,
            List<String> reasons) {}
}

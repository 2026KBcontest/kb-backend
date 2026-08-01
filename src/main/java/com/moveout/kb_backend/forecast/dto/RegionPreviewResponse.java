package com.moveout.kb_backend.forecast.dto;

import java.util.List;

/**
 * 지역별 시세 미리보기.
 *
 * <p>자취 목표 설정 화면에서 지역과 계약 방식을 고르는 순간, 분석을 돌리기 전에
 * "그 동네가 대략 얼마인지" 를 보여주기 위한 값이다.
 *
 * <p>금액은 전부 서버가 계산해서 내려준다. 중개수수료 계산식을 화면에 복사해두면
 * 화면과 분석 결과의 숫자가 서로 달라지는 순간이 온다.
 */
public record RegionPreviewResponse(List<Item> regions) {

    /**
     * @param region 자치구 이름 (예: 강남구)
     * @param jeonse 전세로 살 때
     * @param wolse 월세로 살 때
     */
    public record Item(String region, Cost jeonse, Cost wolse) {}

    /**
     * @param deposit 보증금 (월세는 1,000만원 고정)
     * @param monthlyRent 매달 내는 월세 (전세는 0)
     * @param brokerageFee 중개수수료
     * @param requiredAmount 처음에 있어야 하는 돈 (보증금 + 월세 2개월 + 중개수수료)
     */
    public record Cost(
            Long deposit, Long monthlyRent, Long brokerageFee, Long requiredAmount) {}
}

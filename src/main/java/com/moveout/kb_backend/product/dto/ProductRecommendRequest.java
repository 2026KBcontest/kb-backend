package com.moveout.kb_backend.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 추천 요청.
 *
 * <p>금액은 화면이 이미 계산해서 보낸다. 서버가 다시 계산하면 화면에 보이는 숫자와
 * 추천 이유의 숫자가 어긋날 수 있다.
 *
 * @param category LOAN | SAVINGS
 * @param neededAmount 더 필요한 금액 (필요 자금 − 자기자본 − 지원금)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductRecommendRequest {

    private String category;
    private Long neededAmount;
    private Long monthlyIncome;
    private Long currentAsset;
}

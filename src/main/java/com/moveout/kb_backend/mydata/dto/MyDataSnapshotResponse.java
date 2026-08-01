package com.moveout.kb_backend.mydata.dto;

import com.moveout.kb_backend.mydata.entity.MyDataSnapshot;

public record MyDataSnapshotResponse(
        Long food,
        Long culture,
        Long shopping,
        Long etc,
        Long fixedCostTransport,
        Long fixedCostTelecom,
        Long fixedCostInsurance,
        Long fixedCostSubscription,
        Long fixedCostLoanInterest,
        Long fixedCostHousing,
        Long assetDeposit,
        Long assetSaving,
        Long assetInvestment,
        Long assetLoan,
        Long assetRemainingRepayment,
        /** 기존 대출의 매달 상환액(원금+이자). DSR 계산에 쓴다 */
        Long assetMonthlyRepayment,
        /**
         * 마지막으로 연동한 시각.
         *
         * <p>화면의 "마지막 업데이트 ○○" 가 이 값을 쓴다. 전에는 안 내려줘서 프론트가
         * 브라우저에만 기록했고, 다시 로그인하면 시각이 없어 <b>현재 시각을 지어냈다</b>.
         * 그래서 며칠 전에 연동해둔 계정도 매번 "방금 업데이트" 로 보였다.
         * {@code SimulationResultResponse} 가 이미 같은 방식으로 내려주고 있다.
         */
        java.time.LocalDateTime updatedAt) {

    public static MyDataSnapshotResponse from(MyDataSnapshot snapshot) {
        return new MyDataSnapshotResponse(
                snapshot.getFood(),
                snapshot.getCulture(),
                snapshot.getShopping(),
                snapshot.getEtc(),
                snapshot.getFixedCostTransport(),
                snapshot.getFixedCostTelecom(),
                snapshot.getFixedCostInsurance(),
                snapshot.getFixedCostSubscription(),
                snapshot.getFixedCostLoanInterest(),
                snapshot.getFixedCostHousing(),
                snapshot.getAssetDeposit(),
                snapshot.getAssetSaving(),
                snapshot.getAssetInvestment(),
                snapshot.getAssetLoan(),
                snapshot.getAssetRemainingRepayment(),
                snapshot.getAssetMonthlyRepayment(),
                snapshot.getUpdatedAt());
    }
}

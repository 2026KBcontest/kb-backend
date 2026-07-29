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
        Long assetRemainingRepayment) {

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
                snapshot.getAssetRemainingRepayment());
    }
}

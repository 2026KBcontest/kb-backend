package com.moveout.kb_backend.mydata.entity;

import com.moveout.kb_backend.user.entity.BaseTimeEntity;
import com.moveout.kb_backend.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MyDataSnapshot extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // 소비정보 (monthlyConsumption)
    private Long food;
    private Long culture;
    private Long shopping;
    private Long etc;
    private Long fixedCostTransport;
    private Long fixedCostTelecom;
    private Long fixedCostInsurance;
    private Long fixedCostSubscription;
    private Long fixedCostLoanInterest;
    private Long fixedCostHousing;

    // 자산정보 (asset)
    private Long assetDeposit;
    private Long assetSaving;
    private Long assetInvestment;
    private Long assetLoan;
    private Long assetRemainingRepayment;

    public MyDataSnapshot(User user) {
        this.user = user;
    }

    public void updateConsumption(
            Long food,
            Long culture,
            Long shopping,
            Long etc,
            Long fixedCostTransport,
            Long fixedCostTelecom,
            Long fixedCostInsurance,
            Long fixedCostSubscription,
            Long fixedCostLoanInterest,
            Long fixedCostHousing) {
        this.food = food;
        this.culture = culture;
        this.shopping = shopping;
        this.etc = etc;
        this.fixedCostTransport = fixedCostTransport;
        this.fixedCostTelecom = fixedCostTelecom;
        this.fixedCostInsurance = fixedCostInsurance;
        this.fixedCostSubscription = fixedCostSubscription;
        this.fixedCostLoanInterest = fixedCostLoanInterest;
        this.fixedCostHousing = fixedCostHousing;
    }

    public void updateAsset(
            Long assetDeposit,
            Long assetSaving,
            Long assetInvestment,
            Long assetLoan,
            Long assetRemainingRepayment) {
        this.assetDeposit = assetDeposit;
        this.assetSaving = assetSaving;
        this.assetInvestment = assetInvestment;
        this.assetLoan = assetLoan;
        this.assetRemainingRepayment = assetRemainingRepayment;
    }
}

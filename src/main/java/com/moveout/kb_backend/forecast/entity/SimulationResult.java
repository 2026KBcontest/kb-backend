package com.moveout.kb_backend.forecast.entity;

import com.moveout.kb_backend.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SimulationResult {

    @Id
    private UUID userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    private String region;

    @Enumerated(EnumType.STRING)
    private HousingType housingType;

    private Long deposit;
    private Long monthlyRent;
    private Long brokerageFee;
    private Long requiredAmount;
    private Long currentAsset;
    private Long monthlySavingCapacity;
    private boolean isFallbackApplied;
    private Integer estimatedMonths;
    private LocalDate predictedStartDate;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public SimulationResult(User user) {
        this.user = user;
    }

    public void update(
            String region,
            HousingType housingType,
            Long deposit,
            Long monthlyRent,
            Long brokerageFee,
            Long requiredAmount,
            Long currentAsset,
            Long monthlySavingCapacity,
            boolean isFallbackApplied,
            Integer estimatedMonths,
            LocalDate predictedStartDate) {
        this.region = region;
        this.housingType = housingType;
        this.deposit = deposit;
        this.monthlyRent = monthlyRent;
        this.brokerageFee = brokerageFee;
        this.requiredAmount = requiredAmount;
        this.currentAsset = currentAsset;
        this.monthlySavingCapacity = monthlySavingCapacity;
        this.isFallbackApplied = isFallbackApplied;
        this.estimatedMonths = estimatedMonths;
        this.predictedStartDate = predictedStartDate;
    }
}

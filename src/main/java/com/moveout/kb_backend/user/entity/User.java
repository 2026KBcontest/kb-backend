package com.moveout.kb_backend.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, unique = true)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Job job;

    @Column(nullable = false)
    private String residenceRegion;

    @Column(nullable = false)
    private String phone;

    private Long monthlyIncome;

    /**
     * 사용자가 스스로 정한 월 저축 목표 (원).
     *
     * <p>SimulationResult 의 monthlySavingCapacity(소득 - 지출)와는 다른 값이다.
     * capacity 는 계산된 최대치이고, 이 값은 실제로 모으기로 한 금액이라
     * 보통 capacity 보다 낮게 잡는다. 화면은 이 값을 기준으로 자취 시점을 다시 계산한다.
     */
    private Long monthlySavingGoal;

    private String refreshToken;

    private Long assets;

    /**
     * 자취 희망 지역.
     *
     * <p>회원가입에서는 받지 않는 값이라 NOT NULL 로 두면 가입 자체가 실패한다.
     * (가입 시점에는 아직 어디서 살지 정하지 않은 사용자가 대부분이다)
     * 자취 시뮬레이션 단계에서 정해지므로 비어 있을 수 있는 값으로 둔다.
     */
    private String desiredRegion;


    public User(String loginId, String password, String email, String name) {
        this.loginId = loginId;
        this.password = password;
        this.email = email;
        this.name = name;
    }

    @Builder
    public User(
        String loginId,
        String password,
        String email,
        String name,
        LocalDate birthDate,
        Gender gender,
        Job job,
        String residenceRegion,
        String phone,
        Long monthlyIncome,
        Long assets,
        String desiredRegion) {
        this.loginId = loginId;
        this.password = password;
        this.email = email;
        this.name = name;
        this.birthDate = birthDate;
        this.gender = gender;
        this.job = job;
        this.residenceRegion = residenceRegion;
        this.phone = phone;
        this.monthlyIncome = monthlyIncome;
        this.assets = assets;
        this.desiredRegion = desiredRegion;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void updateMonthlyIncome(Long monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateProfile(
            String name,
            String email,
            LocalDate birthDate,
            String residenceRegion,
            Gender gender,
            Job job,
            String phone,
            Long monthlyIncome,
            Long assets,
            String desiredRegion,
            Long monthlySavingGoal) {
        if (name != null) {
            this.name = name;
        }
        if (email != null) {
            this.email = email;
        }
        if (birthDate != null) {
            this.birthDate = birthDate;
        }
        if (residenceRegion != null) {
            this.residenceRegion = residenceRegion;
        }
        if (gender != null) {
            this.gender = gender;
        }
        if (job != null) {
            this.job = job;
        }
        if (phone != null) {
            this.phone = phone;
        }
        if (monthlyIncome != null) {
            this.monthlyIncome = monthlyIncome;
        }
        if (assets != null) {
            this.assets = assets;
        }
        if (desiredRegion != null) {
            this.desiredRegion = desiredRegion;
        }
        if (monthlySavingGoal != null) {
            this.monthlySavingGoal = monthlySavingGoal;
        }
    }
}

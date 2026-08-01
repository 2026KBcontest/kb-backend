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

    private String refreshToken;

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
            String phone) {
        this.loginId = loginId;
        this.password = password;
        this.email = email;
        this.name = name;
        this.birthDate = birthDate;
        this.gender = gender;
        this.job = job;
        this.residenceRegion = residenceRegion;
        this.phone = phone;
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
            String phone) {
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
    }
}

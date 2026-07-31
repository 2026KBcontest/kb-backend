package com.moveout.kb_backend.policy.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "youth_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class YouthPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String bizId; // 온통청년 정책 고유 ID

    @Column(nullable = false)
    private String title; // 정책명 (polyBizSjnm)

    @Column(columnDefinition = "TEXT")
    private String summary; // 정책 소개 (polyItcnCn)

    private String ageInfo; // 연령 요건 (ageInfo)

    private String applyUrl; // 신청 URL (rqutUrla)

    @Builder
    public YouthPolicy(String bizId, String title, String summary, String ageInfo, String applyUrl) {
        this.bizId = bizId;
        this.title = title;
        this.summary = summary;
        this.ageInfo = ageInfo;
        this.applyUrl = applyUrl;
    }
}
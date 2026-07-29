package com.moveout.kb_backend.policy.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class YouthPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String policyId;       // 공공데이터/온통청년 고유 정책 ID

    @Column(nullable = false)
    private String title;          // 정책명 (예: 청년월세 특별지원)

    private String category;       // 카테고리 (주거, 금융 등)

    @Column(length = 2000)
    private String description;    // 정책 요약 설명

    private String targetAge;      // 대상 연령 (예: 만 19세 ~ 34세)
    private String region;         // 지원 지역 (예: 전국, 서울)
    private String provider;       // 주관 기관
    private String applyUrl;       // 신청 URL
}
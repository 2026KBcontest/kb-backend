package com.moveout.kb_backend.product.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class KbProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String productId;      // 상품 코드 (예: KB-LOAN-001)

    @Column(nullable = false)
    private String bankName;       // 은행명 (KB국민은행)

    @Column(nullable = false)
    private String productName;    // 상품명

    private String category;       // 전세자금대출, 주택담보대출 등

    private BigDecimal minInterestRate; // 최저 금리
    private BigDecimal maxInterestRate; // 최고 금리
    private Long maxLimit;              // 대출 한도

    @Column(length = 1000)
    private String targetDescription;   // 지원 대상 조건

    private String officialUrl;         // 상품 공시 URL
}
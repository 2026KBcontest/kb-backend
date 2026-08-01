package com.moveout.kb_backend.product.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @GetMapping
    public ResponseEntity<?> getProducts() {
        List<Map<String, Object>> productList = List.of(
            Map.of(
                "productId", "kb-youth-jeonse-loan",
                "name", "KB 청년 맞춤형 전세자금대출",
                "category", "LOAN",
                "tag", "최저 금리",
                "specs", List.of(
                    Map.of("label", "최저 금리", "value", "2.40%"),
                    Map.of("label", "예상 한도", "value", "2억원"),
                    Map.of("label", "상환 기간", "value", "최대 10년")
                ),
                "calc", Map.of(
                    "maxLimit", 200000000L,
                    "minRate", 2.4,
                    "maxRate", 4.1,
                    "maxYears", 10
                ),
                "link", "https://obank.kbstar.com/quics?page=C018020"
            ),
            Map.of(
                "productId", "kb-youth-doyak-savings",
                "name", "KB 청년도약계좌",
                "category", "SAVINGS",
                "tag", "청년 우대",
                "specs", List.of(
                    Map.of("label", "최고 금리", "value", "6.00%"),
                    Map.of("label", "월 납입한도", "value", "70만원"),
                    Map.of("label", "가입 기간", "value", "5년")
                ),
                "calc", Map.of(
                    "maxLimit", 700000L,
                    "minRate", 4.5,
                    "maxRate", 6.0,
                    "maxYears", 5
                ),
                "link", "https://obank.kbstar.com/quics?page=C018021"
            )
        );

        return ResponseEntity.ok(Map.of("products", productList));
    }
}
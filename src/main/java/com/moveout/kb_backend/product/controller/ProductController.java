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
        List<Map<String, Object>> mockProducts = List.of(
            Map.of(
                "id", "prod-1",
                "name", "KB 청년 맞춤형 전세자금대출",
                "category", "LOAN",
                "maxAmount", 200000000,
                "interestRate", "2.4%"
            ),
            Map.of(
                "id", "prod-2",
                "name", "KB 청년 도약 적금",
                "category", "SAVINGS",
                "maxAmount", 700000,
                "interestRate", "6.0%"
            )
        );
        return ResponseEntity.ok(mockProducts);
    }
}
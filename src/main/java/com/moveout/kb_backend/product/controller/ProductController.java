package com.moveout.kb_backend.product.controller;

import com.moveout.kb_backend.product.dto.ProductRecommendRequest;
import com.moveout.kb_backend.product.dto.ProductResponse;
import com.moveout.kb_backend.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 상품 목록.
     *
     * <p>예전에는 이 컨트롤러가 상품 값을 직접 들고 있었다. 그러면 실제 데이터 파일
     * ({@code data/kb_products.json}) 을 고쳐도 화면이 안 바뀌고, 파일과 화면이 서로 다른
     * 상품을 말하게 된다. 지금은 파일을 읽어서 준다.
     */
    @GetMapping
    public ResponseEntity<ProductResponse> getProducts() {
        return ResponseEntity.ok(productService.getAll());
    }

    /**
     * 상품 목록 + AI 추천.
     *
     * <p>사용자 상황(부족한 금액·소득)을 알아야 고를 근거가 생기므로 POST 로 받는다.
     * 후보가 1개뿐이면 AI 를 부르지 않고 목록만 돌려준다 — 고를 게 없는데 부르면 비용만 나간다.
     */
    @PostMapping("/recommend")
    public ResponseEntity<ProductResponse> recommend(@RequestBody ProductRecommendRequest request) {
        return ResponseEntity.ok(productService.recommend(request));
    }
}

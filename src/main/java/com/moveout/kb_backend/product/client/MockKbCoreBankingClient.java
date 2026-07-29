package com.moveout.kb_backend.product.client;

import com.moveout.kb_backend.product.entity.KbProduct;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component
public class MockKbCoreBankingClient implements KbCoreBankingClient {

    @Override
    public List<KbProduct> fetchKbProducts() {
        return List.of(
            KbProduct.builder()
                .productId("KB-LOAN-001")
                .bankName("KB국민은행")
                .productName("KB 청년 맞춤형 전세자금대출")
                .category("전세자금대출")
                .minInterestRate(new BigDecimal("3.42"))
                .maxInterestRate(new BigDecimal("4.15"))
                .maxLimit(200000000L)
                .targetDescription("만 19세 이상 ~ 만 34세 이하 무주택 청년")
                .officialUrl("https://kbstar.com")
                .build()
        );
    }

    @Override
    public KbProduct fetchKbProductById(String productId) {
        return fetchKbProducts().stream()
            .filter(product -> product.getProductId().equals(productId))
            .findFirst()
            .orElse(null);
    }
}
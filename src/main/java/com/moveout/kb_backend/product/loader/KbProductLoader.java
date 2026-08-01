package com.moveout.kb_backend.product.loader;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * KB 금융상품 원본 데이터를 읽어 들인다.
 *
 * <p>{@code resources/data/kb_products.json} 은 KB 홈페이지에서 정리한 실제 상품 정보다.
 * 예전에는 이 파일이 있는데도 아무도 읽지 않고 컨트롤러에 값을 직접 적어뒀는데,
 * 그러면 파일을 고쳐도 화면이 안 바뀌고 실제 상품과 어긋나게 된다.
 *
 * <p>원본 필드 : productId, bankName, productName, category,
 * minInterestRate, maxInterestRate, maxLimit, targetDescription, officialUrl
 */
@Slf4j
@Component
public class KbProductLoader {

    private static final String PATH = "data/kb_products.json";

    private List<Map<String, Object>> products = List.of();

    @PostConstruct
    @SuppressWarnings("unchecked")
    void load() {
        try (InputStream in = new ClassPathResource(PATH).getInputStream()) {
            products = new ObjectMapper().readValue(in, List.class);
            log.info("KB 금융상품 {}건을 읽었습니다 ({})", products.size(), PATH);
        } catch (IOException | RuntimeException e) {
            // 상품이 없어도 서버는 떠야 한다. 화면은 '불러오지 못했어요' 를 보여주면 된다.
            log.warn("KB 금융상품 파일을 읽지 못했습니다: {}", PATH, e);
            products = List.of();
        }
    }

    public List<Map<String, Object>> findAll() {
        return products;
    }
}

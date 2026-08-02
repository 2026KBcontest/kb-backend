package com.moveout.kb_backend.product.service;

import com.moveout.kb_backend.ai.service.AiPicker;
import com.moveout.kb_backend.product.dto.ProductRecommendRequest;
import com.moveout.kb_backend.product.dto.ProductResponse;
import com.moveout.kb_backend.product.loader.KbProductLoader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * KB 금융상품 조회·추천.
 *
 * <p>원본은 {@code data/kb_products.json} 이고, 화면이 쓰기 좋은 모양으로 바꿔서 내보낸다.
 * <b>금액·금리는 원본 값을 그대로 옮긴다.</b> 여기서 값을 만들어내면 실제 상품과 달라진다.
 *
 * <p>추천은 정책 화면과 같은 방식이다 — 후보 목록을 AI 에게 주고 하나 고르게 한 뒤,
 * 고른 id 가 진짜 목록에 있는지 확인한다. 실패하면 금리 기준 규칙으로 내려간다.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    /**
     * 상환 기간 가정값(년).
     *
     * <p>원본 데이터에 상환 기간이 없다. 그런데 DSR(월 상환액)을 계산하려면 기간이 있어야 한다.
     * 지어낸 값을 상품 정보처럼 보이게 하지 않으려고 상수로 빼두고, 화면에도 '가정' 이라고 밝힌다.
     */
    private static final int ASSUMED_YEARS = 10;

    /**
     * 테스트용 상품의 id 앞머리.
     *
     * <p>DSR 경고 화면처럼 <b>실제 상품으로는 재현하기 어려운 경우</b>를 확인하려고 넣어둔
     * 가짜 상품이 있다. 목록에는 보여야 눌러볼 수 있지만, <b>AI 추천 후보에서는 빼야 한다</b> —
     * 시연 중에 AI 가 "이 상품을 추천합니다" 하고 테스트 데이터를 골라버리면 곤란하다.
     */
    private static final String TEST_PRODUCT_PREFIX = "TEST-";

    private final KbProductLoader kbProductLoader;
    private final AiPicker aiPicker;

    /** 테스트용으로 넣어둔 가짜 상품인지. */
    private boolean isTestProduct(ProductResponse.Item item) {
        return item.productId() != null && item.productId().startsWith(TEST_PRODUCT_PREFIX);
    }

    /** 상품 목록만. 추천은 붙이지 않는다(사용자 상황을 모르면 고를 근거가 없다). */
    public ProductResponse getAll() {
        return new ProductResponse(toItems(), null);
    }

    /**
     * 사용자 상황을 받아 AI 가 하나를 고르게 한다.
     *
     * <p>후보가 1개뿐이면 AI 를 부르지 않는다. 고를 게 없는데 호출하면 크레딧만 나간다.
     */
    public ProductResponse recommend(ProductRecommendRequest request) {
        List<ProductResponse.Item> all = toItems();

        String category = request.getCategory() == null ? "LOAN" : request.getCategory();
        // 테스트 상품은 목록에는 남기고 추천 후보에서만 뺀다
        List<ProductResponse.Item> candidates =
                all.stream()
                        .filter(item -> category.equals(item.category()))
                        .filter(item -> !isTestProduct(item))
                        .toList();

        if (candidates.size() < 2) {
            return new ProductResponse(all, null);
        }

        ProductResponse.Recommendation recommendation = pickByAi(request, candidates);
        if (recommendation == null) {
            recommendation = pickByRule(candidates);
        }
        return new ProductResponse(all, recommendation);
    }

    /* ---------- 원본 → 화면용 ---------- */

    private List<ProductResponse.Item> toItems() {
        List<ProductResponse.Item> items = new ArrayList<>();

        for (Map<String, Object> raw : kbProductLoader.findAll()) {
            String name = str(raw.get("productName"));
            String rawCategory = str(raw.get("category"));
            Double minRate = dbl(raw.get("minInterestRate"));
            Double maxRate = dbl(raw.get("maxInterestRate"));
            Long maxLimit = lng(raw.get("maxLimit"));

            if (name == null) {
                continue; // 이름 없는 상품은 카드로 만들 수 없다
            }

            List<ProductResponse.Spec> specs = new ArrayList<>();
            if (minRate != null && maxRate != null) {
                specs.add(new ProductResponse.Spec("금리", String.format("연 %.2f~%.2f%%", minRate, maxRate)));
            }
            if (maxLimit != null) {
                specs.add(new ProductResponse.Spec("최대 한도", formatLimit(maxLimit)));
            }

            /* 원본에 상환 기간이 적혀 있으면 그 값을 쓰고, 없으면 10년으로 가정한다.
               가정값일 때만 '가정' 이라고 밝힌다 — 적혀 있는 값까지 가정이라고 하면
               어느 게 실제 상품 정보인지 구분이 안 된다.

               개월로도 적을 수 있게 둔 이유 — 1년 미만인 상품을 년 단위로는 못 적는다. */
            Integer months = intg(raw.get("repaymentMonths"));
            Integer declaredYears = intg(raw.get("repaymentYears"));

            double years;
            String periodText;
            if (months != null) {
                years = months / 12.0;
                periodText = months + "개월";
            } else if (declaredYears != null) {
                years = declaredYears;
                periodText = declaredYears + "년";
            } else {
                years = ASSUMED_YEARS;
                periodText = ASSUMED_YEARS + "년 가정";
            }
            specs.add(new ProductResponse.Spec("상환 기간", periodText));

            items.add(
                    new ProductResponse.Item(
                            str(raw.get("productId")),
                            name,
                            toCategoryCode(rawCategory),
                            rawCategory, // 배지에는 원본 분류를 그대로 (전세자금대출 / 주택담보대출)
                            str(raw.get("targetDescription")),
                            List.copyOf(specs),
                            new ProductResponse.Calc(maxLimit, minRate, maxRate, years),
                            str(raw.get("officialUrl"))));
        }

        // 금리가 낮은 순 — 사용자가 먼저 보고 싶은 순서다
        items.sort(Comparator.comparingDouble(item ->
                item.calc().minRate() == null ? Double.MAX_VALUE : item.calc().minRate()));
        return items;
    }

    /** 원본 분류를 화면이 쓰는 코드로 바꾼다. 적금·예금이 생기면 여기에 추가하면 된다. */
    private String toCategoryCode(String rawCategory) {
        if (rawCategory == null) {
            return "LOAN";
        }
        return rawCategory.contains("적금") || rawCategory.contains("예금") ? "SAVINGS" : "LOAN";
    }

    /** 2억원처럼 사람이 읽는 단위로. 원 단위 숫자를 그대로 보여주면 자릿수를 세게 된다. */
    private String formatLimit(long won) {
        if (won >= 100_000_000L && won % 100_000_000L == 0) {
            return (won / 100_000_000L) + "억원";
        }
        if (won >= 100_000_000L) {
            return String.format("%.1f억원", won / 100_000_000.0);
        }
        return String.format("%,d만원", won / 10_000L);
    }

    /* ---------- 추천 ---------- */

    private ProductResponse.Recommendation pickByAi(
            ProductRecommendRequest request, List<ProductResponse.Item> candidates) {

        String userBlock =
                """
                더 필요한 금액 : %s
                월 소득 : %s
                지금 모은 돈 : %s
                """
                        .formatted(
                                money(request.getNeededAmount()),
                                money(request.getMonthlyIncome()),
                                money(request.getCurrentAsset()));

        List<String> lines = new ArrayList<>();
        for (ProductResponse.Item item : candidates) {
            lines.add(
                    String.format(
                            "id=%s | %s | 금리 연 %.2f~%.2f%% | 최대 한도 %s | 대상 %s",
                            item.productId(),
                            item.name(),
                            item.calc().minRate() == null ? 0 : item.calc().minRate(),
                            item.calc().maxRate() == null ? 0 : item.calc().maxRate(),
                            formatLimit(item.calc().maxLimit() == null ? 0 : item.calc().maxLimit()),
                            item.target() == null ? "확인 필요" : item.target()));
        }

        List<String> ids = candidates.stream().map(ProductResponse.Item::productId).toList();

        AiPicker.Pick pick =
                aiPicker.pick(
                        userBlock,
                        lines,
                        ids,
                        "위 상품 중 이 사용자에게 가장 맞는 것 하나를 고르고, 금리와 한도, 대상 조건을 근거로 이유를 설명해주세요.");

        if (pick == null) {
            return null;
        }
        return new ProductResponse.Recommendation(
                "ai", pick.pickId(), pick.headline(), pick.reasons());
    }

    /** AI 가 없을 때 — 금리가 가장 낮은 상품. source 를 "rule" 로 정직하게 표기한다. */
    private ProductResponse.Recommendation pickByRule(List<ProductResponse.Item> candidates) {
        ProductResponse.Item best = candidates.get(0); // 이미 금리 낮은 순으로 정렬돼 있다

        List<String> reasons = new ArrayList<>();
        if (best.calc().minRate() != null) {
            reasons.add(String.format("후보 중 최저 금리예요 (연 %.2f%%부터)", best.calc().minRate()));
        }
        if (best.calc().maxLimit() != null) {
            reasons.add("최대 " + formatLimit(best.calc().maxLimit()) + "까지 받을 수 있어요");
        }
        reasons.add("실제 금리와 한도는 심사 결과에 따라 달라져요");

        return new ProductResponse.Recommendation(
                "rule", best.productId(), best.name() + "을(를) 먼저 확인해보세요", reasons);
    }

    /* ---------- 값 꺼내기 ---------- */

    private String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private Double dbl(Object v) {
        return v instanceof Number n ? n.doubleValue() : null;
    }

    private Long lng(Object v) {
        return v instanceof Number n ? n.longValue() : null;
    }

    private Integer intg(Object v) {
        return v instanceof Number n ? n.intValue() : null;
    }

    private String money(Long v) {
        return v == null ? "확인 필요" : String.format("%,d원", v);
    }
}

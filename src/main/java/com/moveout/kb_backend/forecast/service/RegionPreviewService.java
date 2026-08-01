package com.moveout.kb_backend.forecast.service;

import com.moveout.kb_backend.forecast.dto.RegionPreviewResponse;
import com.moveout.kb_backend.forecast.region.RegionHousingFee;
import com.moveout.kb_backend.forecast.region.RegionHousingFeeLoader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 지역별 시세 미리보기.
 *
 * <p>분석을 돌리기 전에 "고른 동네가 대략 얼마인지" 를 화면에 보여주기 위한 값을 만든다.
 * 계산식은 {@link ForecastService} 와 같은 것을 쓴다 — 미리보기와 분석 결과의 금액이
 * 다르면 사용자는 둘 중 무엇을 믿어야 할지 알 수 없다.
 */
@Service
@RequiredArgsConstructor
public class RegionPreviewService {

    /** 월세는 보증금을 1,000만원으로 고정한다 (ForecastService 와 같은 기준). */
    private static final long WOLSE_FIXED_DEPOSIT = 10_000_000L;

    private final RegionHousingFeeLoader regionHousingFeeLoader;

    public RegionPreviewResponse getAll() {
        List<RegionPreviewResponse.Item> items = new ArrayList<>();

        for (Map.Entry<String, RegionHousingFee> entry : regionHousingFeeLoader.findAll().entrySet()) {
            RegionHousingFee fee = entry.getValue();
            items.add(
                    new RegionPreviewResponse.Item(
                            entry.getKey(),
                            cost(fee.depositWon(), 0L),
                            cost(WOLSE_FIXED_DEPOSIT, fee.monthlyRentWon())));
        }

        // 이름순으로 정렬해 화면에서 순서가 매번 바뀌지 않게 한다 (Map 은 순서를 보장하지 않는다)
        items.sort(Comparator.comparing(RegionPreviewResponse.Item::region));
        return new RegionPreviewResponse(items);
    }

    private RegionPreviewResponse.Cost cost(long deposit, long monthlyRent) {
        long brokerageFee = ForecastService.calculateBrokerageFee(deposit, monthlyRent);
        long requiredAmount = deposit + monthlyRent * 2 + brokerageFee;
        return new RegionPreviewResponse.Cost(deposit, monthlyRent, brokerageFee, requiredAmount);
    }
}

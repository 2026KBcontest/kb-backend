package com.moveout.kb_backend.forecast.service;

import com.moveout.kb_backend.ai.service.AiPicker;
import com.moveout.kb_backend.common.exception.BusinessException;
import com.moveout.kb_backend.forecast.dto.RegionOptionResponse;
import com.moveout.kb_backend.forecast.entity.HousingType;
import com.moveout.kb_backend.forecast.entity.SimulationResult;
import com.moveout.kb_backend.forecast.region.AdjacentRegions;
import com.moveout.kb_backend.forecast.region.RegionHousingFee;
import com.moveout.kb_backend.forecast.region.RegionHousingFeeLoader;
import com.moveout.kb_backend.forecast.repository.SimulationResultRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지역 변경 추천.
 *
 * <p><b>역할 분담</b>
 *
 * <pre>
 *   인접 구 목록      코드 (AdjacentRegions)   — 변하지 않는 사실
 *   절감액·기간 계산  코드 (이 클래스)          — 틀리면 안 되는 숫자
 *   어디를 권할지     AI                        — 최저가가 늘 정답은 아니다
 *   왜 그런지 설명    AI                        — 사용자 상황마다 다르다
 * </pre>
 *
 * <p>AI 에게 산수를 시키지 않는다. 금융 서비스에서 틀린 금액은 그 자체로 사고다.
 * AI 는 이 클래스가 만들어준 후보 목록 안에서 고르고 문장을 쓴다.
 *
 * <p>기준값(자기자본·월 저축 여력)은 이미 저장된 시뮬레이션 결과에서 가져온다.
 * 같은 기준으로 계산해야 "지금보다 몇 개월 빨라진다" 가 말이 된다.
 */
@Service
@RequiredArgsConstructor
public class RegionSwitchService {

    /** 월세는 보증금을 1,000만원으로 고정한다 (ForecastService 와 같은 기준). */
    private static final long WOLSE_FIXED_DEPOSIT = 10_000_000L;

    /**
     * 보여줄 만한 차이의 기준. 전세와 월세를 따로 둔다.
     *
     * <p>전세는 보증금이 통째로 줄어 초기 자금이 수천만원 단위로 빠진다.
     * 월세는 보증금이 1,000만원 고정이라 초기 차이는 수십만원뿐이고, 실제로 아껴지는 건
     * <b>매달 나가는 월세</b>다. 초기 자금 하나로만 걸러내면 월세 추천이 전부 사라진다.
     */
    private static final long MIN_SAVED_DEPOSIT = 1_000_000L; // 전세 : 초기 자금 100만원

    private static final long MIN_SAVED_RENT = 30_000L; // 월세 : 매달 3만원

    /** 기준을 넘는 후보가 하나도 없을 때, 차이가 작더라도 보여줄 개수. */
    private static final int FALLBACK_SIZE = 3;

    private final SimulationResultRepository simulationResultRepository;
    private final RegionHousingFeeLoader regionHousingFeeLoader;
    private final AiPicker aiPicker;

    @Transactional(readOnly = true)
    public RegionOptionResponse getOptions(UUID userId) {
        SimulationResult result =
                simulationResultRepository
                        .findById(userId)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "SIMULATION_004", "저장된 시뮬레이션 결과가 없습니다."));

        String currentRegion = result.getRegion();
        HousingType housingType = result.getHousingType();

        // 엔티티가 Long 이라 값이 비어 있을 수 있다. 0 으로 두고 계산은 이어간다.
        long currentAsset = result.getCurrentAsset() == null ? 0L : result.getCurrentAsset();
        long capacity =
                result.getMonthlySavingCapacity() == null ? 0L : result.getMonthlySavingCapacity();
        long currentRequired = result.getRequiredAmount() == null ? 0L : result.getRequiredAmount();

        RegionOptionResponse.Current current =
                new RegionOptionResponse.Current(
                        currentRegion,
                        housingType,
                        result.getRequiredAmount(),
                        result.getEstimatedMonths());

        Integer currentMonths = result.getEstimatedMonths();

        long currentRent = result.getMonthlyRent() == null ? 0L : result.getMonthlyRent();

        /* 인접 구를 전부 계산한다. 비싼 곳도 버리지 않는다.
           "주변에서 지금이 제일 저렴하다" 는 것도 사용자에게 의미 있는 답이라,
           그 말을 하려면 비싼 쪽 숫자도 알고 있어야 한다. */
        List<RegionOptionResponse.Candidate> cheaper = new ArrayList<>();
        List<RegionOptionResponse.Candidate> pricier = new ArrayList<>();
        for (String neighbor : AdjacentRegions.of(currentRegion)) {
            RegionHousingFee fee = regionHousingFeeLoader.find(neighbor).orElse(null);
            if (fee == null) {
                continue; // 시세 데이터가 없는 지역은 지어내지 않고 건너뛴다
            }

            long deposit;
            long monthlyRent;
            if (housingType == HousingType.JEONSE) {
                deposit = fee.depositWon();
                monthlyRent = 0L;
            } else {
                deposit = WOLSE_FIXED_DEPOSIT;
                monthlyRent = fee.monthlyRentWon();
            }

            long brokerageFee = ForecastService.calculateBrokerageFee(deposit, monthlyRent);
            long requiredAmount = deposit + monthlyRent * 2 + brokerageFee;

            long savedAmount = currentRequired - requiredAmount;
            long monthlyRentSaved = currentRent - monthlyRent;

            Integer months = estimateMonths(requiredAmount, currentAsset, capacity);
            Integer shorten =
                    (currentMonths != null && months != null) ? currentMonths - months : null;

            RegionOptionResponse.Candidate candidate =
                    new RegionOptionResponse.Candidate(
                            neighbor,
                            deposit,
                            monthlyRent,
                            requiredAmount,
                            savedAmount,
                            monthlyRentSaved,
                            months,
                            shorten);

            boolean isCheaper =
                    housingType == HousingType.JEONSE ? savedAmount > 0 : monthlyRentSaved > 0;
            (isCheaper ? cheaper : pricier).add(candidate);
        }

        // 아끼는 자리가 다르므로 정렬 기준도 다르다
        Comparator<RegionOptionResponse.Candidate> byBenefit =
                housingType == HousingType.JEONSE
                        ? Comparator.comparingLong(RegionOptionResponse.Candidate::savedAmount)
                        : Comparator.comparingLong(
                                RegionOptionResponse.Candidate::monthlyRentSaved);
        cheaper.sort(byBenefit.reversed());
        pricier.sort(byBenefit.reversed()); // 비싼 쪽은 '그나마 덜 비싼 순'

        /* 눈에 띄는 차이가 있는 곳만 남긴다.
           단, 전부 걸러지면 차이가 작더라도 상위 몇 개는 보여준다.
           "주변에 더 싼 곳이 아예 없다" 와 "차이가 작다" 는 다른 이야기이고,
           고를지 말지는 사용자가 금액을 보고 판단할 몫이다. */
        List<RegionOptionResponse.Candidate> candidates =
                cheaper.stream().filter(c -> isMeaningful(c, housingType)).toList();

        if (candidates.isEmpty() && !cheaper.isEmpty()) {
            candidates = cheaper.subList(0, Math.min(FALLBACK_SIZE, cheaper.size()));
        }

        RegionOptionResponse.Recommendation recommendation;
        if (candidates.isEmpty()) {
            // 옮길 곳이 없으면 AI 를 부를 이유가 없다 (호출 한 번이 곧 비용이다)
            recommendation = alreadyCheapest(currentRegion, pricier, housingType);
        } else {
            // AI 가 고르게 해보고, 실패하면 규칙 기반으로 내려간다
            RegionOptionResponse.Recommendation byAi =
                    pickByAi(currentRegion, housingType, currentRequired, currentMonths, candidates);
            recommendation = byAi != null ? byAi : pickByRule(candidates, housingType);
        }

        return new RegionOptionResponse(current, candidates, recommendation);
    }

    /**
     * ForecastService 와 같은 규칙으로 도달 개월을 계산한다.
     *
     * @return 이미 모았으면 0, 저축 여력이 없어 도달 불가면 null
     */
    private Integer estimateMonths(long requiredAmount, long currentAsset, long capacity) {
        if (currentAsset >= requiredAmount) {
            return 0;
        }
        if (capacity <= 0) {
            return null;
        }
        long remaining = requiredAmount - currentAsset;
        return (int) Math.ceil((double) remaining / capacity);
    }

    /**
     * AI 가 후보 중 하나를 고르게 한다.
     *
     * <p><b>여기서 AI 가 하는 일</b> — 규칙은 무조건 "가장 많이 아끼는 곳" 을 고르지만,
     * 그게 늘 좋은 추천은 아니다. 5천만원 아끼자고 생활권을 크게 옮기는 것보다,
     * 조금 덜 아껴도 지금 목표 시점을 충분히 앞당기는 곳이 나을 수 있다.
     * 그 판단과 설명이 AI 의 몫이다. 금액과 개월 수는 이미 계산해서 넘겨준다.
     *
     * @return 실패하거나 목록 밖을 고르면 null (부르는 쪽이 규칙 기반으로 내려간다)
     */
    private RegionOptionResponse.Recommendation pickByAi(
            String currentRegion,
            HousingType housingType,
            long currentRequired,
            Integer currentMonths,
            List<RegionOptionResponse.Candidate> candidates) {

        String userBlock =
                """
                지금 목표 지역 : %s (%s)
                필요한 초기 자금 : %,d원
                지금 계획대로면 자취까지 : %s
                """
                        .formatted(
                                currentRegion,
                                housingType == HousingType.JEONSE ? "전세" : "월세",
                                currentRequired,
                                currentMonths == null ? "계산 불가" : currentMonths + "개월");

        List<String> lines = new ArrayList<>();
        for (RegionOptionResponse.Candidate c : candidates) {
            lines.add(
                    String.format(
                            "id=%s | 초기 자금 %,d원 (지금보다 %,d원 적음) | 월세 %,d원 (지금보다 %,d원 적음) | 자취까지 %s%s",
                            c.region(),
                            c.requiredAmount(),
                            c.savedAmount(),
                            c.monthlyRent(),
                            c.monthlyRentSaved(),
                            c.estimatedMonths() == null ? "계산 불가" : c.estimatedMonths() + "개월",
                            c.shortenMonths() != null && c.shortenMonths() > 0
                                    ? " (" + c.shortenMonths() + "개월 단축)"
                                    : ""));
        }

        List<String> ids = candidates.stream().map(RegionOptionResponse.Candidate::region).toList();

        AiPicker.Pick pick =
                aiPicker.pick(
                        userBlock,
                        lines,
                        ids,
                        housingType == HousingType.JEONSE
                                ? "위 지역 중 이 사용자에게 옮겨볼 만한 곳 하나를 고르고, 초기 자금과 자취 시점이 어떻게 달라지는지 설명해주세요."
                                : "위 지역 중 이 사용자에게 옮겨볼 만한 곳 하나를 고르고, 매달 월세가 얼마나 줄어드는지 설명해주세요.");

        if (pick == null) {
            return null;
        }
        return new RegionOptionResponse.Recommendation(
                "ai", pick.pickId(), pick.headline(), pick.reasons());
    }

    /**
     * 옮길 만한 곳이 없을 때 — "지금이 이미 제일 저렴하다" 고 알려준다.
     *
     * <p>빈 화면을 보여주면 사용자는 기능이 고장 났다고 생각한다. 그런데 이건 고장이 아니라
     * <b>좋은 소식</b>이다. 이미 잘 고른 것이므로 그렇게 말해준다.
     * pickRegion 은 지금 지역 자신이 된다.
     */
    private RegionOptionResponse.Recommendation alreadyCheapest(
            String currentRegion,
            List<RegionOptionResponse.Candidate> pricier,
            HousingType housingType) {

        List<String> reasons = new ArrayList<>();
        if (pricier.isEmpty()) {
            reasons.add("비교할 인접 지역 정보가 아직 없어요");
        } else {
            reasons.add(String.format("붙어 있는 %d개 지역이 모두 지금보다 비싸요", pricier.size()));

            // 그나마 가장 가까운 대안이 얼마나 더 드는지 — 숫자가 있어야 납득이 된다
            RegionOptionResponse.Candidate closest = pricier.get(0);
            long gap =
                    housingType == HousingType.JEONSE
                            ? -closest.savedAmount()
                            : -closest.monthlyRentSaved();

            if (gap == 0) {
                // 시세가 같은 지역이 있다 (실제로 성동구-동대문구가 그렇다).
                // "0원 더 필요해요" 는 말이 안 되므로 다르게 적는다.
                reasons.add(
                        String.format(
                                "%s는 지금과 비용이 같아 옮길 이유가 없어요", closest.region()));
            } else if (housingType == HousingType.JEONSE) {
                reasons.add(
                        String.format(
                                "가장 가까운 %s도 초기 자금이 %,d원 더 필요해요", closest.region(), gap));
            } else {
                reasons.add(
                        String.format(
                                "가장 가까운 %s도 월세가 매달 %,d원 더 나가요", closest.region(), gap));
            }
        }
        reasons.add("지금 지역을 유지하면서 저축 속도를 높이는 편이 나아요");

        return new RegionOptionResponse.Recommendation(
                "rule",
                currentRegion,
                currentRegion + subjectParticle(currentRegion) + " 주변에서 가장 저렴해요",
                reasons);
    }

    /**
     * 받침 여부에 따라 주격 조사를 고른다 ("강북구가" / "여의도동이").
     *
     * <p>"강북구이(가)" 처럼 쓰면 사람이 쓴 문장으로 안 보인다. 화면에 그대로 나가는 글이라
     * 이 정도는 맞춰준다.
     */
    private String subjectParticle(String word) {
        if (word == null || word.isEmpty()) {
            return "가";
        }
        char last = word.charAt(word.length() - 1);
        if (last < 0xAC00 || last > 0xD7A3) {
            return "가"; // 한글이 아니면 판단하지 않는다
        }
        boolean hasFinalConsonant = (last - 0xAC00) % 28 != 0;
        return hasFinalConsonant ? "이" : "가";
    }

    /** 보여줄 만한 차이인지. 전세는 초기 자금, 월세는 매달 나가는 돈으로 판단한다. */
    private boolean isMeaningful(RegionOptionResponse.Candidate c, HousingType housingType) {
        return housingType == HousingType.JEONSE
                ? c.savedAmount() >= MIN_SAVED_DEPOSIT
                : c.monthlyRentSaved() >= MIN_SAVED_RENT;
    }

    /**
     * AI 가 붙기 전까지 쓰는 규칙 기반 추천 — 가장 많이 아끼는 곳.
     *
     * <p>source 를 "rule" 로 정직하게 표기한다. AI 가 고른 것처럼 보이게 하지 않는다.
     * Perplexity 연동이 끝나면 이 자리를 AI 응답이 대체한다.
     */
    private RegionOptionResponse.Recommendation pickByRule(
            List<RegionOptionResponse.Candidate> candidates, HousingType housingType) {
        if (candidates.isEmpty()) {
            return null;
        }
        RegionOptionResponse.Candidate best = candidates.get(0);

        List<String> reasons = new ArrayList<>();
        if (housingType == HousingType.JEONSE) {
            reasons.add(String.format("필요한 초기 자금이 %,d원 줄어들어요", best.savedAmount()));
        } else {
            reasons.add(String.format("월세가 매달 %,d원 덜 나가요", best.monthlyRentSaved()));
            // 1년 기준을 함께 보여준다. 매달 몇 만원은 작아 보여도 1년이면 체감이 다르다.
            reasons.add(String.format("1년이면 %,d원 차이예요", best.monthlyRentSaved() * 12));
        }
        if (best.shortenMonths() != null && best.shortenMonths() > 0) {
            reasons.add(String.format("자취 시점이 %d개월 앞당겨져요", best.shortenMonths()));
        }
        reasons.add("지금 지역과 붙어 있어 생활권이 크게 바뀌지 않아요");

        return new RegionOptionResponse.Recommendation(
                "rule", best.region(), best.region() + "도 함께 살펴보세요", reasons);
    }
}

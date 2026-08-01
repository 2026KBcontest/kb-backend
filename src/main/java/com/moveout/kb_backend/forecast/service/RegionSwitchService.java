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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        List<RegionOptionResponse.Pick> picks;
        if (candidates.isEmpty()) {
            // 옮길 곳이 없으면 AI 를 부를 이유가 없다 (호출 한 번이 곧 비용이다)
            recommendation = alreadyCheapest(currentRegion, pricier, housingType);
            picks = List.of();
        } else {
            picks = buildPicks(currentRegion, housingType, currentRequired, currentMonths, candidates);

            /* 대표 추천은 picks 중 하나를 그대로 쓴다 — 따로 고르면 화면끼리 다른 말을 하게 된다.

               '생활 여건' 칸이 있으면 그쪽을 대표로 삼는다. 홈에는 한 줄만 들어가는데,
               "가장 저렴한 곳" 은 정렬 1위라 굳이 대표로 뽑지 않아도 목록 맨 앞에서 보인다.
               반면 여건을 보고 고른 칸은 여기서 안 보여주면 시뮬레이션 화면까지 들어가야 한다.

               source 가 "ai" 인 첫 칸을 고르면 안 된다 — similar 가 ① 과 겹쳐 사라진 경우
               2순위(alt)가 대표로 올라가서, 홈이 "2순위" 를 대표 추천으로 내보내게 된다. */
            RegionOptionResponse.Pick head =
                    picks.stream()
                            .filter(p -> "similar".equals(p.kind()))
                            .findFirst()
                            .orElse(picks.get(0));
            recommendation =
                    new RegionOptionResponse.Recommendation(
                            head.source(), head.region(), head.headline(), head.reasons());
        }

        return new RegionOptionResponse(current, candidates, recommendation, picks);
    }

    /**
     * 성격이 다른 추천 세 칸을 만든다.
     *
     * <pre>
     *   ① 가장 저렴한 곳          계산   절감액 1위
     *   ② 생활 여건이 비슷한 곳    AI     지하철·도심 접근성·상권을 보고 고름
     *   ③ 그다음으로 볼 만한 곳    AI     ② 를 고를 때 함께 받은 2순위 (altId)
     * </pre>
     *
     * <p><b>① 을 AI 에게 맡기지 않는 이유</b> — 이미 계산이 끝난 값이라 정답이 하나다.
     * 정답이 있는 걸 LLM 에게 물으면 느리고, 비용이 들고, 가끔 틀린다.
     * AI 는 우리 데이터에 없는 것(생활 여건)을 판단할 때만 쓴다.
     *
     * <p><b>③ 이 '가장 빨리 갈 수 있는 곳' 이 아닌 이유</b> — 그건 ① 과 늘 같은 지역이다.
     *
     * <pre>
     *   자취 개월 = ceil((필요 초기자금 − 모은 돈) ÷ 월 저축액)
     * </pre>
     *
     * 필요 자금이 적을수록 개월이 줄어드는 단조 관계라, <b>절감액 1위 = 단축 1위</b> 가
     * 예외 없이 성립한다 (월세도 마찬가지 — 월세가 싸면 필요 자금도 적다).
     * 처음엔 그렇게 만들었다가 세 번째 칸이 매번 겹쳐 사라지는 걸 보고 축을 바꿨다.
     *
     * <p>③ 은 AI 응답의 {@code altId} 를 쓴다. <b>호출은 여전히 한 번</b>이다 —
     * 애초에 "1순위와 2순위를 함께 달라" 는 형식이라 한 번에 둘이 온다.
     *
     * <p><b>같은 지역이 두 칸에 겹치면 뒤 칸을 버린다.</b> 세 칸에 같은 이름이 나오면
     * 나눠 보여준 의미가 없고, 오히려 선택지가 많은 것처럼 착각하게 만든다.
     * 겹쳐서 한 칸만 남는다면 그건 "그 지역이 모든 면에서 낫다" 는 뜻이므로 그대로 보여준다.
     */
    private List<RegionOptionResponse.Pick> buildPicks(
            String currentRegion,
            HousingType housingType,
            long currentRequired,
            Integer currentMonths,
            List<RegionOptionResponse.Candidate> candidates) {

        List<RegionOptionResponse.Pick> picks = new ArrayList<>();

        // ① 가장 저렴한 곳 — candidates 는 이미 절감액 순으로 정렬돼 있다
        picks.add(cheapestPick(candidates.get(0), housingType));

        /* ②③ 후보가 하나뿐이면 고를 게 없으므로 AI 를 부르지 않는다 (호출 = 비용) */
        if (candidates.size() > 1) {
            AiPicker.Pick ai =
                    askAi(currentRegion, housingType, currentRequired, currentMonths, candidates);

            if (ai != null) {
                picks.add(
                        new RegionOptionResponse.Pick(
                                "similar",
                                "생활 여건이 비슷한 곳",
                                "ai",
                                ai.pickId(),
                                soften(ai.headline(), ai.pickId()),
                                ai.reasons()));

                /* ③ 2순위. AiPicker 가 목록 밖이거나 1순위와 같은 altId 는 이미 걸러서 null 로 준다.
                   설명은 AI 가 따로 써주지 않으므로 우리가 아는 숫자로 채운다 —
                   고른 건 AI 지만 문장은 계산값이라, 없는 근거를 지어내지 않는다. */
                candidates.stream()
                        .filter(c -> c.region().equals(ai.altId()))
                        .findFirst()
                        .map(c -> altPick(c, housingType))
                        .ifPresent(picks::add);
            }
        }

        // 겹치는 지역 제거 — 앞 칸이 이긴다 (①②③ 순으로 확실한 것부터)
        List<RegionOptionResponse.Pick> unique = new ArrayList<>();
        for (RegionOptionResponse.Pick p : picks) {
            if (unique.stream().noneMatch(kept -> kept.region().equals(p.region()))) {
                unique.add(p);
            }
        }
        return unique;
    }

    /** ① 가장 저렴한 곳 — 절감액 1위. 금액이 근거라 설명이 짧아도 된다. */
    private RegionOptionResponse.Pick cheapestPick(
            RegionOptionResponse.Candidate c, HousingType housingType) {

        List<String> reasons = new ArrayList<>();
        if (housingType == HousingType.JEONSE) {
            reasons.add(String.format("초기 자금이 %,d원 적게 들어요", c.savedAmount()));
        } else {
            reasons.add(String.format("월세가 매달 %,d원 적어요", c.monthlyRentSaved()));
            // 1년 기준을 함께 적는다. 매달 몇 만원은 작아 보여도 1년이면 체감이 다르다
            reasons.add(String.format("1년이면 %,d원 차이예요", c.monthlyRentSaved() * 12));
        }
        if (c.shortenMonths() != null && c.shortenMonths() > 0) {
            reasons.add(String.format("자취 시점은 %d개월 빨라져요", c.shortenMonths()));
        }
        reasons.add("바로 옆이라 생활권이 크게 바뀌지 않아요");

        return new RegionOptionResponse.Pick(
                "cheapest",
                "가장 저렴한 곳",
                "rule",
                c.region(),
                c.region() + "가 주변에서 가장 저렴해요",
                reasons);
    }

    /**
     * ③ 그다음으로 볼 만한 곳 — AI 가 2순위로 고른 지역.
     *
     * <p>고른 주체는 AI 라 {@code source} 는 "ai" 로 둔다. 다만 이유 문장은 AI 가 따로 써주지
     * 않으므로 우리가 아는 숫자로만 채운다. 그럴듯한 설명을 지어내는 것보다 낫다.
     */
    private RegionOptionResponse.Pick altPick(
            RegionOptionResponse.Candidate c, HousingType housingType) {

        List<String> reasons = new ArrayList<>();
        if (housingType == HousingType.JEONSE) {
            reasons.add(String.format("초기 자금이 %,d원 적게 들어요", c.savedAmount()));
        } else {
            reasons.add(String.format("월세가 매달 %,d원 적어요", c.monthlyRentSaved()));
        }
        if (c.estimatedMonths() != null) {
            reasons.add(
                    c.estimatedMonths() == 0
                            ? "지금 모은 돈으로 바로 들어갈 수 있어요"
                            : String.format("지금 저축 속도로 %d개월이면 돼요", c.estimatedMonths()));
        }
        reasons.add("AI 가 두 번째로 꼽은 곳이에요");

        return new RegionOptionResponse.Pick(
                "alt",
                "그다음으로 볼 만한 곳",
                "ai",
                c.region(),
                c.region() + "도 후보로 남겨둘 만해요",
                reasons);
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
     * ② 생활 여건이 지금 지역과 가장 비슷한 곳을 AI 가 고르게 한다.
     *
     * <p><b>왜 이 칸만 AI 인가</b> — "가장 싸다", "가장 빠르다" 는 우리가 이미 계산했다.
     * 답이 하나뿐인 걸 LLM 에게 물으면 느리고, 비용이 들고, 가끔 틀린다.
     * 반면 <b>지하철 노선·도심 접근성·상권·대학가</b> 같은 건 우리 DB 에 아예 없다.
     * 우리가 못 하는 일만 AI 에게 넘긴다.
     *
     * <p>기준을 프롬프트에 못 박아두는 이유 — "알아서 비슷한 곳" 이라고만 하면 실행할 때마다
     * 근거가 달라져 시연 중에 다른 답이 나온다. 무엇을 보고 골랐는지도 설명할 수 없게 된다.
     *
     * <p>1순위(pickId)와 2순위(altId)를 한 번에 받는다. 그래서 카드 두 칸을 만들면서도
     * AI 호출은 한 번뿐이다.
     *
     * @return 실패하거나 목록 밖을 고르면 null (②③ 칸이 그냥 빠진다)
     */
    private AiPicker.Pick askAi(
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

        /* '가장 싼 곳' 은 이미 다른 칸에 있다. 여기서 또 그걸 고르면 두 칸이 겹쳐 사라진다.
           그래서 무엇을 보고 골라야 하는지를 못 박는다. */
        String instruction =
                """
                위 지역 중 %s(과)와 생활 여건이 가장 비슷한 곳 하나를 골라주세요.
                아래 네 가지를 기준으로 보세요. 값이 가장 싼 곳을 고르는 자리가 아닙니다.
                  1) 지하철 노선이 겹치거나 환승 없이 이어지는지
                  2) 서울 도심(광화문·강남·여의도)까지 걸리는 시간이 비슷한지
                  3) 생활 상권(대형마트·번화가)이 비슷한 수준인지
                  4) 대학가·직장 밀집지 여부가 비슷한지
                reasons 에는 위 기준 중 실제로 근거가 된 것을 구체적으로 적으세요.
                (예: "4호선이 그대로 이어져 환승 없이 다닐 수 있어요")
                금액은 %s
                altId 에는 그다음으로 볼 만한 곳을 하나 더 적어주세요. 없으면 비워두세요.
                지금 지역을 그만두라는 뜻이 아니라 비교해볼 선택지를 보여주는 것입니다.
                """
                        .formatted(
                                currentRegion,
                                housingType == HousingType.JEONSE
                                        ? "초기 자금 절감액을 한 줄만 덧붙이세요."
                                        : "매달 월세 절감액을 한 줄만 덧붙이세요.");

        return aiPicker.pick(userBlock, lines, ids, instruction);
    }

    /**
     * 지시조로 온 headline 을 제안조로 되돌린다.
     *
     * <p>프롬프트로 "제안조로 쓰라" 고 일러도 모델은 종종 "○○구로 옮기시면 됩니다" 처럼 답한다.
     * 사는 곳을 옮기는 건 사용자가 정할 일이고, 화면이 그걸 시키는 것처럼 읽히면 안 된다.
     * 그래서 프롬프트에만 맡기지 않고 여기서 한 번 더 거른다.
     *
     * <p>고치지 않고 <b>통째로 갈아끼우는</b> 이유 — 어미만 바꾸면 "옮기시는 건 어떠세요"
     * 처럼 여전히 이사를 전제한 문장이 남는다. 안전한 문장으로 바꾸는 편이 확실하다.
     * 이유(reasons)는 AI 가 쓴 그대로 두므로 설명의 값어치는 잃지 않는다.
     */
    private static final List<String> PUSHY =
            List.of("옮기세요", "옮기시면", "이사하세요", "이사하시면", "하셔야", "해야 합니다",
                    "하시면 됩니다", "하시길", "추천합니다", "권합니다", "선택하세요", "바꾸세요");

    private String soften(String headline, String pickId) {
        if (headline == null || headline.isBlank()) {
            return pickId + "도 함께 살펴보세요";
        }
        for (String word : PUSHY) {
            if (headline.contains(word)) {
                log.info("AI headline 이 지시조라 제안조로 바꿨습니다: {}", headline);
                return pickId + "도 함께 살펴보시겠어요?";
            }
        }
        return headline;
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

    /* 예전의 pickByRule 은 cheapestPick 이 대신한다.
       "가장 많이 아끼는 곳" 을 고르는 일이 그대로 ① 칸이 됐고,
       AI 실패 시의 대비책이라는 성격도 사라졌다 (② 칸이 실패하면 그 칸만 빠진다). */
}

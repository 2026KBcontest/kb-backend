package com.moveout.kb_backend.forecast.controller;

import com.moveout.kb_backend.forecast.dto.RegionOptionResponse;
import com.moveout.kb_backend.forecast.dto.RegionPreviewResponse;
import com.moveout.kb_backend.forecast.dto.SetGoalRequest;
import com.moveout.kb_backend.forecast.dto.SimulationResultResponse;
import com.moveout.kb_backend.forecast.service.ForecastService;
import com.moveout.kb_backend.forecast.service.RegionPreviewService;
import com.moveout.kb_backend.forecast.service.RegionSwitchService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forecast")
@RequiredArgsConstructor
public class ForecastController {

    private final ForecastService forecastService;
    private final RegionSwitchService regionSwitchService;
    private final RegionPreviewService regionPreviewService;

    /**
     * 지역별 시세 미리보기 — 분석을 돌리기 전에 화면에서 보여준다.
     *
     * <p>로그인 없이도 볼 수 있어야 한다. 공개된 시세 정보이고, 개인 데이터가 섞이지 않는다.
     */
    @GetMapping("/regions")
    public RegionPreviewResponse getRegions() {
        return regionPreviewService.getAll();
    }

    @GetMapping
    public SimulationResultResponse getResult(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return forecastService.getResult(userId);
    }

    /**
     * 지역 변경 추천 — 지금 목표 지역과 붙어 있으면서 더 저렴한 구를 알려준다.
     *
     * <p>저장된 시뮬레이션 결과가 있어야 한다(SIMULATION_004). 같은 자기자본·저축 여력으로
     * 계산해야 "지금보다 몇 개월 빨라진다" 를 비교할 수 있기 때문이다.
     */
    @GetMapping("/region-options")
    public RegionOptionResponse getRegionOptions(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        return regionSwitchService.getOptions(userId);
    }

    @PostMapping("/simulate")
    public SimulationResultResponse simulate(Authentication authentication, @Valid @RequestBody SetGoalRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return forecastService.simulate(userId, request);
    }
}

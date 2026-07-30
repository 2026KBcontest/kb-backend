package com.moveout.kb_backend.forecast.controller;

import com.moveout.kb_backend.forecast.dto.SetGoalRequest;
import com.moveout.kb_backend.forecast.dto.SimulationResultResponse;
import com.moveout.kb_backend.forecast.service.ForecastService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/forecast")
@RequiredArgsConstructor
public class ForecastController {

    private final ForecastService forecastService;

    @PostMapping("/simulate")
    public SimulationResultResponse simulate(Authentication authentication, @Valid @RequestBody SetGoalRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        return forecastService.simulate(userId, request);
    }
}

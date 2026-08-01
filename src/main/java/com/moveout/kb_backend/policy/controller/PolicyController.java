package com.moveout.kb_backend.policy.controller;

import com.moveout.kb_backend.policy.dto.PolicyRequest;
import com.moveout.kb_backend.policy.dto.PolicyResponse;
import com.moveout.kb_backend.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/policy")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    @PostMapping("/recommend")
    public ResponseEntity<PolicyResponse> getPolicyRecommendation(@RequestBody PolicyRequest request) {
        PolicyResponse response = policyService.getRecommendedPolicies(request);
        return ResponseEntity.ok(response);
    }
}
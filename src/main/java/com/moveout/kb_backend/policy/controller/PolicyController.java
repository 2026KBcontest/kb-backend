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
        PolicyResponse response = policyService.getRecommendedPolicy(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sync")
    public ResponseEntity<String> syncPolicies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int display) {
        int count = policyService.syncYouthPoliciesFromApi(page, display);
        return ResponseEntity.ok("온통청년 정책 " + count + "건 DB 동기화 완료");
    }
}
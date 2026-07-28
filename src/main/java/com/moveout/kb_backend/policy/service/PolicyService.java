package com.moveout.kb_backend.policy.service;

import com.moveout.kb_backend.policy.client.YouthPolicyClient;
import com.moveout.kb_backend.policy.dto.PolicyRequest;
import com.moveout.kb_backend.policy.dto.PolicyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final YouthPolicyClient youthPolicyClient;

    public PolicyResponse recommendPolicy(PolicyRequest request) {
        // YouthPolicyClient를 통해 외부 API/추천 로직 호출
        return youthPolicyClient.fetchPolicyData(request);
    }
}
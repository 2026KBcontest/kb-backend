package com.moveout.kb_backend.policy.service;

import com.moveout.kb_backend.policy.client.YouthPolicyClient;
import com.moveout.kb_backend.policy.dto.PolicyRequest;
import com.moveout.kb_backend.policy.dto.PolicyResponse;
import com.moveout.kb_backend.policy.dto.YouthPolicyXmlResponse;
import com.moveout.kb_backend.policy.entity.YouthPolicy;
import com.moveout.kb_backend.policy.repository.YouthPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final YouthPolicyClient youthPolicyClient;
    private final YouthPolicyRepository youthPolicyRepository;

    /**
     * 온통청년 API 호출하여 DB에 Seed 데이터 동기화/저장
     */
    @Transactional
    public int syncYouthPoliciesFromApi(int pageIndex, int displayCount) {
        YouthPolicyXmlResponse xmlResponse = youthPolicyClient.fetchRawPolicyData(pageIndex, displayCount);

        if (xmlResponse == null || xmlResponse.getEmpList() == null) {
            return 0;
        }

        int savedCount = 0;
        for (YouthPolicyXmlResponse.PolicyItem item : xmlResponse.getEmpList()) {
            // 이미 존재하는 정책(bizId)이면 저장 건너뜀 (중복 저장 방지)
            if (youthPolicyRepository.findByBizId(item.getBizId()).isPresent()) {
                continue;
            }

            YouthPolicy policy = YouthPolicy.builder()
                    .bizId(item.getBizId())
                    .title(item.getPolyBizSjnm())
                    .summary(item.getPolyItcnCn())
                    .ageInfo(item.getAgeInfo())
                    .applyUrl(item.getRqutUrla())
                    .build();

            youthPolicyRepository.save(policy);
            savedCount++;
        }

        return savedCount;
    }

    /**
     * 추천/조회 서비스 API
     */
    @Transactional(readOnly = true)
    public PolicyResponse getRecommendedPolicy(PolicyRequest request) {
        List<YouthPolicy> policies = youthPolicyRepository.findAll();

        if (!policies.isEmpty()) {
            YouthPolicy policy = policies.get(0);
            return new PolicyResponse(
                    policy.getTitle(),
                    policy.getSummary(),
                    policy.getAgeInfo(),
                    policy.getApplyUrl()
            );
        }

        // DB에 데이터가 없을 경우 외부 API 직접 호출 Fallback
        return youthPolicyClient.fetchPolicyData(request);
    }
}
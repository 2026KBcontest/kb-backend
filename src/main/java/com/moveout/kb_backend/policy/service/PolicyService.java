package com.moveout.kb_backend.policy.service;

import com.moveout.kb_backend.policy.client.YouthPolicyClient;
import com.moveout.kb_backend.policy.dto.PolicyItemDto;
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

    @Transactional
    public int syncYouthPoliciesFromApi(int pageIndex, int displayCount) {
        YouthPolicyXmlResponse xmlResponse = youthPolicyClient.fetchRawPolicyData(pageIndex, displayCount);

        if (xmlResponse == null || xmlResponse.getEmpList() == null) {
            return 0;
        }

        int savedCount = 0;
        for (YouthPolicyXmlResponse.PolicyItem item : xmlResponse.getEmpList()) {
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

    @Transactional(readOnly = true)
    public PolicyResponse getRecommendedPolicy(PolicyRequest request) {
        List<YouthPolicy> dbPolicies = youthPolicyRepository.findAll();
        List<PolicyItemDto> items = new ArrayList<>();

        if (!dbPolicies.isEmpty()) {
            int limit = Math.min(dbPolicies.size(), 4); // 최대 4개 추천
            for (int i = 0; i < limit; i++) {
                YouthPolicy p = dbPolicies.get(i);
                
                // summary 25자 이내 처리
                String rawSummary = p.getSummary() != null ? p.getSummary() : "청년 주거 지원 정책입니다.";
                String desc = rawSummary.length() > 25 ? rawSummary.substring(0, 22) + "..." : rawSummary;

                items.add(PolicyItemDto.builder()
                        .policyId(p.getBizId())
                        .policyName(p.getTitle())
                        .description(desc)
                        .eligibility(p.getAgeInfo() != null ? p.getAgeInfo() : "만 19~34세 대상")
                        .status("신청 가능")
                        .link(p.getApplyUrl())
                        .build());
            }
        } else {
            // DB 데이터가 없는 경우 Mock 목록 2개 구성
            items.add(PolicyItemDto.builder()
                    .policyId("jeonse-loan")
                    .policyName("청년 버팀목 전세자금대출")
                    .description("최대 1.2억원 대출 가능")
                    .eligibility("만 19~34세 / 연소득 5천만원 이하")
                    .status("신청 가능")
                    .link("https://nhuf.molit.go.kr/")
                    .build());

            items.add(PolicyItemDto.builder()
                    .policyId("monthly-rent")
                    .policyName("서울시 청년 월세 지원")
                    .description("월 최대 20만원 × 12개월")
                    .eligibility("만 19~39세 / 중위소득 150% 이하")
                    .status("조건 확인 필요")
                    .link("https://youth.seoul.go.kr/")
                    .build());
        }

        String aiReason = "현재 연령·소득 조건 및 자취 목표를 종합 고려할 때 맞춤 정책을 우선 신청하는 것이 유리합니다.";
        return new PolicyResponse(items, aiReason);
    }
}
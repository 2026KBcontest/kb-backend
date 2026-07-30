package com.moveout.kb_backend.policy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyResponse {

    private String title;       // 정책명
    private String summary;     // 정책 요약
    private String ageInfo;     // 연령 자격 조건
    private String applyUrl;    // 신청 페이지 링크
}
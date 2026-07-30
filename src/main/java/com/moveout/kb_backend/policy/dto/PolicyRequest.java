package com.moveout.kb_backend.policy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PolicyRequest {

    private String region;      // 지역 (예: 서울, 경기)
    private Long income;        // 소득 (원 단위: 예 - 2000000)
    private String birthDate;   // 생년월일 (YYYY-MM-DD)
}
package com.moveout.kb_backend.policy.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PolicyRequest {

    private String birthDate; // 생년월일
    private String job; // 직업
    private String region; // 현재 거주지역
    private Long income; // 월 소득
    private Long asset; // 보유 자산
}
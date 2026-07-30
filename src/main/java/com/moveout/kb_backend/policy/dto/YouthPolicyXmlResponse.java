package com.moveout.kb_backend.policy.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JacksonXmlRootElement(localName = "empSprtList")
public class YouthPolicyXmlResponse {

    @JacksonXmlProperty(localName = "totalCnt")
    private Integer totalCnt;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "emp")
    private List<PolicyItem> empList;

    @Getter
    @Setter
    public static class PolicyItem {

        @JacksonXmlProperty(localName = "bizId")
        private String bizId; // 정책 ID

        @JacksonXmlProperty(localName = "polyBizSjnm")
        private String polyBizSjnm; // 정책명

        @JacksonXmlProperty(localName = "polyItcnCn")
        private String polyItcnCn; // 정책 소개

        @JacksonXmlProperty(localName = "ageInfo")
        private String ageInfo; // 연령 요건

        @JacksonXmlProperty(localName = "rqutUrla")
        private String rqutUrla; // 신청 사이트 주소
    }
}
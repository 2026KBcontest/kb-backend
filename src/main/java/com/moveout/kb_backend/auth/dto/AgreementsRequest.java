package com.moveout.kb_backend.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AgreementsRequest {

    @NotNull
    @AssertTrue(message = "개인정보 처리방침에 동의해야 합니다.")
    private Boolean privacyAgreed;

    @NotNull
    @AssertTrue(message = "마이데이터 수집·이용에 동의해야 합니다.")
    private Boolean mydataAgreed;

    @NotNull
    private Boolean marketingAgreed;
}

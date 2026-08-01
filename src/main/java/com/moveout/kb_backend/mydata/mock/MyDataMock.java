package com.moveout.kb_backend.mydata.mock;

public record MyDataMock(MonthlyConsumption monthlyConsumption, Asset asset) {

    public record MonthlyConsumption(Long food, Long culture, Long shopping, Long etc, FixedCost fixedCost) {}

    public record FixedCost(
            Long transport, Long telecom, Long insurance, Long subscription, Long loanInterest, Long housing) {}

    /**
     * @param remainingRepayment 아직 갚지 않은 원금
     * @param monthlyRepayment 매달 갚는 원금 + 이자.
     *     <p>DSR 은 '연간 원리금 상환액 ÷ 연소득' 이라 이 값이 있어야 계산할 수 있다.
     *     남은 원금(remainingRepayment)만으로는 매달 얼마씩 갚는지 알 수 없다 —
     *     500만원을 3개월에 갚았는지 3년에 걸쳐 갚았는지 구분되지 않기 때문이다.
     */
    public record Asset(
            Long deposit,
            Long saving,
            Long investment,
            Long loan,
            Long remainingRepayment,
            Long monthlyRepayment) {}
}

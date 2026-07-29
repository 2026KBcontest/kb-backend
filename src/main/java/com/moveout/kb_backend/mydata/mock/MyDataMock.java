package com.moveout.kb_backend.mydata.mock;

public record MyDataMock(MonthlyConsumption monthlyConsumption, Asset asset) {

    public record MonthlyConsumption(Long food, Long culture, Long shopping, Long etc, FixedCost fixedCost) {}

    public record FixedCost(
            Long transport, Long telecom, Long insurance, Long subscription, Long loanInterest, Long housing) {}

    public record Asset(Long deposit, Long saving, Long investment, Long loan, Long remainingRepayment) {}
}

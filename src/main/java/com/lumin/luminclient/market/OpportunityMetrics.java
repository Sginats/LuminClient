package com.lumin.luminclient.market;

/** Complete cost-aware accounting for a proposed market trade. */
public record OpportunityMetrics(long quantity, long acquisitionCost, long liquidationProceeds, long grossProfit,
                                 long fees, long slippage, long netProfit, long liquidity, double netProfitPercent) {
    public OpportunityMetrics {
        if (quantity < 0 || acquisitionCost < 0 || liquidationProceeds < 0 || fees < 0 || slippage < 0 || liquidity < 0) {
            throw new IllegalArgumentException("market metrics cannot be negative");
        }
    }
}

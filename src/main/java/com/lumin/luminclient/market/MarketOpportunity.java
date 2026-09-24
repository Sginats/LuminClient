package com.lumin.luminclient.market;

import java.util.List;
import java.util.Objects;

/** Immutable, cost-aware market opportunity. A positive gross margin is not sufficient for profitability. */
public record MarketOpportunity(String productId, long quantity, OpportunityMetrics metrics, FreshnessState freshness,
                                List<String> reasons) {
    public MarketOpportunity {
        productId = Objects.requireNonNull(productId, "productId");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        metrics = Objects.requireNonNull(metrics, "metrics");
        freshness = Objects.requireNonNull(freshness, "freshness");
        reasons = List.copyOf(Objects.requireNonNull(reasons, "reasons"));
    }

    public boolean isProfitable() {
        return freshness.fresh() && metrics.netProfit() > 0;
    }
}

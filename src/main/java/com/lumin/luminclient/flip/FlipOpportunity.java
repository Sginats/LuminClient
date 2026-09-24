package com.lumin.luminclient.flip;

import com.lumin.luminclient.market.MarketOpportunity;

/** Immutable presentation and compatibility adapter for a cost-aware market opportunity. */
public final class FlipOpportunity {

    public enum Type { BAZAAR_MARGIN, BIN_SNIPE }

    public final Type type;
    public final String display;
    public final String productId;
    public final long buyAt;
    public final long sellAt;
    public final long profitPerUnit;
    public final double profitPercent;
    public final long suggestedUnits;
    public final long volumeHint;
    public final MarketOpportunity marketOpportunity;

    public FlipOpportunity(Type type, String display, String productId, long buyAt, long sellAt,
                           long profitPerUnit, double profitPercent, long suggestedUnits, long volumeHint) {
        this(type, display, productId, buyAt, sellAt, profitPerUnit, profitPercent, suggestedUnits, volumeHint, null);
    }

    public FlipOpportunity(Type type, String display, String productId, long buyAt, long sellAt,
                           long profitPerUnit, double profitPercent, long suggestedUnits, long volumeHint,
                           MarketOpportunity marketOpportunity) {
        this.type = type;
        this.display = display;
        this.productId = productId;
        this.buyAt = buyAt;
        this.sellAt = sellAt;
        this.profitPerUnit = profitPerUnit;
        this.profitPercent = profitPercent;
        this.suggestedUnits = suggestedUnits;
        this.volumeHint = volumeHint;
        this.marketOpportunity = marketOpportunity;
    }

    public long totalProfit() {
        if (profitPerUnit <= 0 || suggestedUnits <= 0) {
            return 0L;
        }
        return suggestedUnits > Long.MAX_VALUE / profitPerUnit ? Long.MAX_VALUE : profitPerUnit * suggestedUnits;
    }
}

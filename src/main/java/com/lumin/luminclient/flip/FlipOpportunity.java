package com.lumin.luminclient.flip;

/**
 * One actionable flip suggestion.
 */
public final class FlipOpportunity {

    public enum Type { BAZAAR_MARGIN, BIN_SNIPE }

    public final Type type;
    public final String display;      // short human name (product id or item name)
    public final double buyAt;        // price to buy at
    public final double sellAt;       // expected price to sell at
    public final double profitPerUnit;
    public final double profitPercent;
    public final long suggestedUnits; // how many the budget allows (bazaar)
    public final long volumeHint;     // liquidity hint (bazaar volume)

    public FlipOpportunity(Type type,
                           String display,
                           double buyAt,
                           double sellAt,
                           double profitPerUnit,
                           double profitPercent,
                           long suggestedUnits,
                           long volumeHint) {
        this.type = type;
        this.display = display;
        this.buyAt = buyAt;
        this.sellAt = sellAt;
        this.profitPerUnit = profitPerUnit;
        this.profitPercent = profitPercent;
        this.suggestedUnits = suggestedUnits;
        this.volumeHint = volumeHint;
    }

    public double totalProfit() {
        return profitPerUnit * Math.max(1, suggestedUnits);
    }
}

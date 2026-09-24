package com.lumin.luminclient.market;

/** One executable Bazaar order level, expressed in whole coins and items. */
public record OrderLevel(long pricePerUnit, long quantity) {
    public OrderLevel {
        if (pricePerUnit <= 0) {
            throw new IllegalArgumentException("pricePerUnit must be positive");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
    }
}

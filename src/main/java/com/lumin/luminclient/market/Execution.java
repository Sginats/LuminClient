package com.lumin.luminclient.market;

/** Quantity-aware order-book execution result. Totals saturate at {@link Long#MAX_VALUE}. */
public record Execution(long requestedQuantity, long filledQuantity, long totalCoins) {
    public Execution {
        if (requestedQuantity < 0 || filledQuantity < 0 || totalCoins < 0 || filledQuantity > requestedQuantity) {
            throw new IllegalArgumentException("invalid execution");
        }
    }

    public boolean fullyFilled() {
        return filledQuantity == requestedQuantity;
    }

    public long unfilledQuantity() {
        return requestedQuantity - filledQuantity;
    }

    public double averagePricePerUnit() {
        return filledQuantity == 0 ? 0.0 : (double) totalCoins / filledQuantity;
    }
}

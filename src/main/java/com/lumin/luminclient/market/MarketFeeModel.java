package com.lumin.luminclient.market;

/**
 * Bazaar fee policy: liquidation proceeds are charged {@code sellFeeRate}, rounded up
 * to whole coins. Acquisition has no modeled fee in this layer.
 */
public record MarketFeeModel(double sellFeeRate) {
    public MarketFeeModel {
        if (!Double.isFinite(sellFeeRate) || sellFeeRate < 0.0 || sellFeeRate > 1.0) {
            throw new IllegalArgumentException("sellFeeRate must be between zero and one");
        }
    }

    public static MarketFeeModel standardBazaar() {
        return new MarketFeeModel(0.01);
    }

    public long liquidationFee(long grossProceeds) {
        if (grossProceeds <= 0) {
            return 0L;
        }
        double rawFee = grossProceeds * sellFeeRate;
        return rawFee >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) Math.ceil(rawFee);
    }
}

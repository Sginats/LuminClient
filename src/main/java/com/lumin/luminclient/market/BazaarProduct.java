package com.lumin.luminclient.market;

import java.util.Objects;

/** Immutable Bazaar product, including executable order-book data and API reference prices. */
public record BazaarProduct(String productId, OrderBook orderBook, long referenceBuyPrice, long referenceSellPrice,
                            long reportedBuyVolume, long reportedSellVolume) {
    public BazaarProduct {
        productId = Objects.requireNonNull(productId, "productId");
        orderBook = Objects.requireNonNull(orderBook, "orderBook");
        if (referenceBuyPrice < 0 || referenceSellPrice < 0 || reportedBuyVolume < 0 || reportedSellVolume < 0) {
            throw new IllegalArgumentException("prices and volumes cannot be negative");
        }
    }
}

package com.lumin.luminclient.market;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Immutable Bazaar response snapshot. */
public record BazaarSnapshot(Instant lastUpdated, Map<String, BazaarProduct> products) {
    public BazaarSnapshot {
        lastUpdated = Objects.requireNonNull(lastUpdated, "lastUpdated");
        products = Map.copyOf(Objects.requireNonNull(products, "products"));
    }
}

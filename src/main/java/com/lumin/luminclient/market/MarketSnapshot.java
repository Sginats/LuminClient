package com.lumin.luminclient.market;

import java.time.Instant;
import java.util.Objects;

/** A timestamped immutable market data boundary for strategy evaluation. */
public record MarketSnapshot(Instant capturedAt, BazaarSnapshot bazaar) {
    public MarketSnapshot {
        capturedAt = Objects.requireNonNull(capturedAt, "capturedAt");
        bazaar = Objects.requireNonNull(bazaar, "bazaar");
    }
}

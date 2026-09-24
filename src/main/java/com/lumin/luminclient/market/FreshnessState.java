package com.lumin.luminclient.market;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Freshness and confidence information associated with an evaluated opportunity. */
public record FreshnessState(Instant observedAt, Duration age, boolean fresh, double confidence, List<String> reasons) {
    public FreshnessState {
        observedAt = Objects.requireNonNull(observedAt, "observedAt");
        age = Objects.requireNonNull(age, "age").isNegative() ? Duration.ZERO : age;
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        reasons = List.copyOf(Objects.requireNonNull(reasons, "reasons"));
    }

    public static FreshnessState assess(Instant observedAt, Instant now, Duration maximumAge, List<String> reasons) {
        Duration age = Duration.between(observedAt, now);
        if (age.isNegative()) {
            age = Duration.ZERO;
        }
        boolean fresh = age.compareTo(maximumAge) <= 0;
        double confidence = fresh ? 1.0 - Math.min(1.0, (double) age.toMillis() / Math.max(1L, maximumAge.toMillis())) : 0.0;
        return new FreshnessState(observedAt, age, fresh, confidence, reasons);
    }
}

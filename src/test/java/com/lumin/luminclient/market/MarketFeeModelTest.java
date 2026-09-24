package com.lumin.luminclient.market;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketFeeModelTest {

    @Test
    void roundsFeesUpToWholeCoins() {
        MarketFeeModel model = MarketFeeModel.standardBazaar();
        assertEquals(1L, model.liquidationFee(1L));
        assertEquals(2L, model.liquidationFee(101L));
        assertEquals(0L, model.liquidationFee(0L));
    }

    @Test
    void calculatesFreshnessConfidenceWithoutNegativeAge() {
        Instant observed = Instant.parse("2026-01-01T00:00:00Z");
        FreshnessState fresh = FreshnessState.assess(observed, observed.plusSeconds(30), Duration.ofMinutes(1), List.of("test"));
        FreshnessState future = FreshnessState.assess(observed.plusSeconds(5), observed, Duration.ofMinutes(1), List.of());

        assertTrue(fresh.fresh());
        assertEquals(0.5, fresh.confidence());
        assertFalse(FreshnessState.assess(observed, observed.plusSeconds(61), Duration.ofMinutes(1), List.of()).fresh());
        assertEquals(Duration.ZERO, future.age());
    }
}

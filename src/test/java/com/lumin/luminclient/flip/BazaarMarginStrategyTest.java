package com.lumin.luminclient.flip;

import com.lumin.luminclient.config.LuminConfig;
import com.lumin.luminclient.market.BazaarProduct;
import com.lumin.luminclient.market.BazaarSnapshot;
import com.lumin.luminclient.market.MarketFeeModel;
import com.lumin.luminclient.market.OrderBook;
import com.lumin.luminclient.market.OrderLevel;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BazaarMarginStrategyTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void evaluatesQuantityWithFeesAndSlippage() {
        BazaarProduct product = product("ENCHANTED_DIAMOND",
                List.of(new OrderLevel(130, 4), new OrderLevel(120, 96)),
                List.of(new OrderLevel(100, 4), new OrderLevel(110, 96)));
        LuminConfig cfg = config(1_000L);

        List<FlipOpportunity> results = strategy().findFlips(snapshot(product, NOW), cfg);

        assertFalse(results.isEmpty());
        FlipOpportunity result = results.get(0);
        assertEquals(5L, result.suggestedUnits); // 5% of the 10-unit book
        assertEquals(510L, result.marketOpportunity.metrics().acquisitionCost());
        assertEquals(640L, result.marketOpportunity.metrics().liquidationProceeds());
        assertEquals(7L, result.marketOpportunity.metrics().fees());
        assertEquals(20L, result.marketOpportunity.metrics().slippage());
        assertEquals(123L, result.marketOpportunity.metrics().netProfit());
        assertTrue(result.marketOpportunity.isProfitable());
    }

    @Test
    void rejectsPreCostMarginsThatAreNotProfitableAfterFees() {
        BazaarProduct product = product("THIN_MARGIN",
                List.of(new OrderLevel(100, 100)), List.of(new OrderLevel(100, 100)));
        assertTrue(strategy().findFlips(snapshot(product, NOW), config(10_000L)).isEmpty());
    }

    @Test
    void rejectsStaleSnapshots() {
        BazaarProduct product = product("STALE", List.of(new OrderLevel(200, 100)), List.of(new OrderLevel(100, 100)));
        assertTrue(strategy().findFlips(snapshot(product, NOW.minusSeconds(61)), config(10_000L)).isEmpty());
    }

    private static BazaarMarginStrategy strategy() {
        return new BazaarMarginStrategy(MarketFeeModel.standardBazaar(), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static BazaarProduct product(String id, List<OrderLevel> buys, List<OrderLevel> sells) {
        return new BazaarProduct(id, new OrderBook(buys, sells), 0L, 0L, 100L, 100L);
    }

    private static BazaarSnapshot snapshot(BazaarProduct product, Instant timestamp) {
        return new BazaarSnapshot(timestamp, Map.of(product.productId(), product));
    }

    private static LuminConfig config(long budget) {
        LuminConfig config = new LuminConfig();
        config.minMarginPercent = 0.0;
        config.minProfitPerFlip = 0.0;
        config.maxBudget = budget;
        config.maxResults = 10;
        return config;
    }
}

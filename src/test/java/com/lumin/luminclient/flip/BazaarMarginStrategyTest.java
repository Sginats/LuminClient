package com.lumin.luminclient.flip;

import com.lumin.luminclient.api.Models;
import com.lumin.luminclient.config.LuminConfig;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BazaarMarginStrategyTest {

    @Test
    void findsProfitableBazaarFlip() {
        Models.BazaarProduct p = new Models.BazaarProduct("ENCHANTED_DIAMOND");
        p.bestSellPrice = 100.0;
        p.bestBuyPrice = 120.0;
        p.buyVolume = 2_000;
        p.sellVolume = 2_000;

        Map<String, Models.BazaarProduct> products = new HashMap<String, Models.BazaarProduct>();
        products.put(p.productId, p);
        Models.BazaarSnapshot snapshot = new Models.BazaarSnapshot(System.currentTimeMillis(), products);

        LuminConfig cfg = new LuminConfig();
        cfg.minMarginPercent = 1.0;
        cfg.minProfitPerFlip = 1.0;
        cfg.maxBudget = 10_000;
        cfg.maxResults = 10;

        List<FlipOpportunity> out = new BazaarMarginStrategy().findFlips(snapshot, cfg);
        assertFalse(out.isEmpty());
        FlipOpportunity top = out.get(0);
        assertEquals(FlipOpportunity.Type.BAZAAR_MARGIN, top.type);
        assertEquals("Enchanted Diamond", top.display);
    }
}

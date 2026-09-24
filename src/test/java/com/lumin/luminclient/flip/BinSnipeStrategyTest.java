package com.lumin.luminclient.flip;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lumin.luminclient.api.HypixelApi;
import com.lumin.luminclient.config.LuminConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BinSnipeStrategyTest {

    @Test
    void usesRobustReferencePriceForSnipes() throws IOException {
        JsonObject page = new JsonObject();
        JsonArray auctions = new JsonArray();
        auctions.add(auction("TARGET", "Item", 100, System.currentTimeMillis() + 120_000));
        auctions.add(auction("TARGET", "Item", 200, System.currentTimeMillis() + 120_000));
        auctions.add(auction("TARGET", "Item", 220, System.currentTimeMillis() + 120_000));
        auctions.add(auction("TARGET", "Item", 240, System.currentTimeMillis() + 120_000));
        auctions.add(auction("TARGET", "Item", 260, System.currentTimeMillis() + 120_000));
        page.add("auctions", auctions);

        HypixelApi api = new HypixelApi("") {
            @Override
            public JsonObject getAuctions(int pageNum) {
                return page;
            }
        };

        LuminConfig cfg = new LuminConfig();
        cfg.enableBinSnipeFlips = true;
        cfg.minBinFlipPercent = 10.0;
        cfg.minProfitPerFlip = 20.0;
        cfg.maxResults = 10;

        List<FlipOpportunity> out = new BinSnipeStrategy().findFlips(api, cfg);
        assertFalse(out.isEmpty());
        assertEquals(FlipOpportunity.Type.BIN_SNIPE, out.get(0).type);
        assertEquals("Item", out.get(0).display);
    }

    private static JsonObject auction(String itemId, String itemName, double price, long end) {
        JsonObject a = new JsonObject();
        a.addProperty("bin", true);
        a.addProperty("item_id", itemId);
        a.addProperty("uuid", itemId + "-" + price);
        a.addProperty("item_name", itemName);
        a.addProperty("starting_bid", price);
        a.addProperty("end", end);
        return a;
    }
}

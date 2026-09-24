package com.lumin.luminclient.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.lumin.luminclient.market.BazaarProduct;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelsTest {

    @Test
    void parsesExecutableSummariesAndKeepsQuickStatusAsReferenceOnly() {
        JsonObject root = new JsonObject();
        root.addProperty("lastUpdated", 1_000L);
        JsonObject product = new JsonObject();
        product.add("buy_summary", levels(new long[][]{{120, 10}, {125, 5}, {-1, 8}, {100, 0}}));
        product.add("sell_summary", levels(new long[][]{{110, 8}, {100, 7}}));
        JsonObject quick = new JsonObject();
        quick.addProperty("buyPrice", 117.6);
        quick.addProperty("sellPrice", 104.2);
        quick.addProperty("buyVolume", 500L);
        quick.addProperty("sellVolume", 700L);
        product.add("quick_status", quick);
        JsonObject products = new JsonObject();
        products.add("ENCHANTED_DIAMOND", product);
        root.add("products", products);

        BazaarProduct parsed = Models.parseBazaar(root).products().get("ENCHANTED_DIAMOND");

        assertEquals(1_000L, Models.parseBazaar(root).lastUpdated().toEpochMilli());
        assertEquals(125L, parsed.orderBook().buyLevels().get(0).pricePerUnit());
        assertEquals(100L, parsed.orderBook().sellLevels().get(0).pricePerUnit());
        assertEquals(118L, parsed.referenceBuyPrice());
        assertEquals(104L, parsed.referenceSellPrice());
        assertEquals(500L, parsed.reportedBuyVolume());
        assertEquals(700L, parsed.reportedSellVolume());
    }

    private static JsonArray levels(long[][] values) {
        JsonArray array = new JsonArray();
        for (long[] value : values) {
            JsonObject level = new JsonObject();
            level.addProperty("pricePerUnit", value[0]);
            level.addProperty("amount", value[1]);
            array.add(level);
        }
        return array;
    }
}

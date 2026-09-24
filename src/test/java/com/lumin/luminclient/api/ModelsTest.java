package com.lumin.luminclient.api;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelsTest {

    @Test
    void parseBazaarParsesQuickStatus() {
        JsonObject root = new JsonObject();
        root.addProperty("lastUpdated", 12345L);
        JsonObject products = new JsonObject();
        JsonObject product = new JsonObject();
        JsonObject quick = new JsonObject();
        quick.addProperty("buyPrice", 120.0);
        quick.addProperty("sellPrice", 100.0);
        quick.addProperty("buyVolume", 500L);
        quick.addProperty("sellVolume", 700L);
        product.add("quick_status", quick);
        products.add("ENCHANTED_DIAMOND", product);
        root.add("products", products);

        Models.BazaarSnapshot snapshot = Models.parseBazaar(root);
        Models.BazaarProduct parsed = snapshot.products.get("ENCHANTED_DIAMOND");

        assertEquals(12345L, snapshot.lastUpdated);
        assertEquals(120.0, parsed.bestBuyPrice);
        assertEquals(100.0, parsed.bestSellPrice);
        assertEquals(500L, parsed.buyVolume);
        assertEquals(700L, parsed.sellVolume);
    }
}

package com.lumin.luminclient.api;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight models for Hypixel SkyBlock market data.
 */
public final class Models {

    private Models() {}

    /** A single bazaar product with its top-of-book orders. */
    public static final class BazaarProduct {
        public final String productId;
        public double bestBuyPrice;   // highest buy order (what a seller gets instantly)
        public double bestSellPrice;  // lowest sell offer (what a buyer pays instantly)
        public long buyVolume;
        public long sellVolume;

        public BazaarProduct(String productId) {
            this.productId = productId;
        }

        /** Margin % if you buy at bestSellPrice and sell at bestBuyPrice. */
        public double marginPercent() {
            if (bestSellPrice <= 0) return 0;
            return (bestBuyPrice - bestSellPrice) / bestSellPrice * 100.0;
        }
    }

    /** Snapshot of the whole bazaar. */
    public static final class BazaarSnapshot {
        public final long lastUpdated;
        public final Map<String, BazaarProduct> products;

        public BazaarSnapshot(long lastUpdated, Map<String, BazaarProduct> products) {
            this.lastUpdated = lastUpdated;
            this.products = products;
        }
    }

    /** A BIN (buy-it-now) auction row. */
    public static final class BinAuction {
        public final String uuid;
        public final String itemName;
        public final String itemId;
        public final double price;
        public final long end;

        public BinAuction(String uuid, String itemName, String itemId, double price, long end) {
            this.uuid = uuid;
            this.itemName = itemName;
            this.itemId = itemId;
            this.price = price;
            this.end = end;
        }
    }

    /** Parsed /v2/skyblock/bazaar response. */
    public static BazaarSnapshot parseBazaar(JsonObject root) {
        long lastUpdated = root.has("lastUpdated") ? root.get("lastUpdated").getAsLong() : 0L;
        Map<String, BazaarProduct> products = new HashMap<String, BazaarProduct>();

        if (root.has("products") && root.get("products").isJsonObject()) {
            JsonObject prods = root.getAsJsonObject("products");
            for (Map.Entry<String, com.google.gson.JsonElement> e : prods.entrySet()) {
                String id = e.getKey();
                JsonObject p = e.getValue().getAsJsonObject();
                BazaarProduct bp = new BazaarProduct(id);

                if (p.has("quick_status") && p.get("quick_status").isJsonObject()) {
                    JsonObject qs = p.getAsJsonObject("quick_status");
                    bp.bestBuyPrice  = qs.has("buyPrice")  ? qs.get("buyPrice").getAsDouble()  : 0;
                    bp.bestSellPrice = qs.has("sellPrice") ? qs.get("sellPrice").getAsDouble() : 0;
                    bp.buyVolume     = qs.has("buyVolume")  ? qs.get("buyVolume").getAsLong()  : 0;
                    bp.sellVolume    = qs.has("sellVolume") ? qs.get("sellVolume").getAsLong() : 0;
                }
                products.put(id, bp);
            }
        }
        return new BazaarSnapshot(lastUpdated, products);
    }
}

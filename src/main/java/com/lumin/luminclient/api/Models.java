package com.lumin.luminclient.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lumin.luminclient.market.BazaarProduct;
import com.lumin.luminclient.market.BazaarSnapshot;
import com.lumin.luminclient.market.OrderBook;
import com.lumin.luminclient.market.OrderLevel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** JSON-to-domain parsing for public Hypixel market responses. */
public final class Models {

    private Models() {
    }

    /** A BIN auction row. Prices are stored as whole coins. */
    public record BinAuction(String uuid, String itemName, String itemId, long price, long end) {
    }

    /** Parses Bazaar summaries into executable order levels, retaining quick-status values only as references. */
    public static BazaarSnapshot parseBazaar(JsonObject root) {
        long updatedEpochMillis = longValue(root, "lastUpdated");
        Instant lastUpdated = updatedEpochMillis > 0 ? Instant.ofEpochMilli(updatedEpochMillis) : Instant.EPOCH;
        Map<String, BazaarProduct> products = new HashMap<String, BazaarProduct>();
        if (root == null || !root.has("products") || !root.get("products").isJsonObject()) {
            return new BazaarSnapshot(lastUpdated, products);
        }

        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("products").entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject product = entry.getValue().getAsJsonObject();
            JsonObject quickStatus = objectValue(product, "quick_status");
            products.put(entry.getKey(), new BazaarProduct(
                    entry.getKey(),
                    new OrderBook(parseLevels(product, "buy_summary"), parseLevels(product, "sell_summary")),
                    priceValue(quickStatus, "buyPrice"),
                    priceValue(quickStatus, "sellPrice"),
                    nonNegative(longValue(quickStatus, "buyVolume")),
                    nonNegative(longValue(quickStatus, "sellVolume"))
            ));
        }
        return new BazaarSnapshot(lastUpdated, products);
    }

    private static List<OrderLevel> parseLevels(JsonObject product, String member) {
        List<OrderLevel> levels = new ArrayList<OrderLevel>();
        if (!product.has(member) || !product.get(member).isJsonArray()) {
            return levels;
        }
        JsonArray summary = product.getAsJsonArray(member);
        for (JsonElement element : summary) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            long price = priceValue(row, "pricePerUnit");
            long quantity = nonNegative(longValue(row, "amount"));
            if (price > 0 && quantity > 0) {
                levels.add(new OrderLevel(price, quantity));
            }
        }
        return levels;
    }

    private static JsonObject objectValue(JsonObject object, String member) {
        return object != null && object.has(member) && object.get(member).isJsonObject()
                ? object.getAsJsonObject(member) : new JsonObject();
    }

    private static long priceValue(JsonObject object, String member) {
        if (object == null || !object.has(member) || !object.get(member).isJsonPrimitive()) {
            return 0L;
        }
        try {
            BigDecimal value = object.get(member).getAsBigDecimal();
            if (value.signum() <= 0) {
                return 0L;
            }
            return value.setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (ArithmeticException | NumberFormatException ignored) {
            return 0L;
        }
    }

    private static long longValue(JsonObject object, String member) {
        if (object == null || !object.has(member) || !object.get(member).isJsonPrimitive()) {
            return 0L;
        }
        try {
            return object.get(member).getAsLong();
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static long nonNegative(long value) {
        return Math.max(0L, value);
    }
}

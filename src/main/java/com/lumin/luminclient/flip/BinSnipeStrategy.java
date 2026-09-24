package com.lumin.luminclient.flip;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.lumin.luminclient.api.HypixelApi;
import com.lumin.luminclient.api.Models;
import com.lumin.luminclient.config.LuminConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BIN snipe flipping.
 *
 * Scans live auctions for Buy-It-Now listings priced significantly below the
 * current lowest BIN for the same item. Buying underpriced BINs and relisting
 * at market is a classic flip.
 *
 * This class only detects the opportunity. Execution is done by the automation layer.
 */
public final class BinSnipeStrategy {

    /** Find the lowest BIN per item id, then flag listings priced well below it. */
    public List<FlipOpportunity> findFlips(HypixelApi api, LuminConfig cfg) throws IOException {
        List<FlipOpportunity> out = new ArrayList<FlipOpportunity>();
        if (!cfg.enableBinSnipeFlips) return out;
        long now = System.currentTimeMillis();

        // Only scan the first few pages for speed; newest auctions are on page 0.
        int maxPages = 3;
        Map<String, List<Models.BinAuction>> byItem = new HashMap<String, List<Models.BinAuction>>();

        for (int page = 0; page < maxPages; page++) {
            JsonObject resp = api.getAuctions(page);
            if (resp == null || !resp.has("auctions")) continue;
            JsonArray auctions = resp.getAsJsonArray("auctions");
            for (JsonElement el : auctions) {
                JsonObject a = el.getAsJsonObject();
                if (!a.has("bin") || !a.get("bin").getAsBoolean()) continue;
                String itemId = a.has("item_id") ? a.get("item_id").getAsString() : null;
                if (itemId == null) continue;
                String uuid = a.has("uuid") ? a.get("uuid").getAsString() : "";
                String name = a.has("item_name") ? a.get("item_name").getAsString() : itemId;
                double price = a.has("starting_bid") ? a.get("starting_bid").getAsDouble() : 0;
                long end = a.has("end") ? a.get("end").getAsLong() : 0L;
                if (price <= 0) continue;
                if (end > 0 && end - now < 30_000L) continue; // too close to expiry

                byItem.computeIfAbsent(itemId, k -> new ArrayList<Models.BinAuction>())
                      .add(new Models.BinAuction(uuid, name, itemId, price, end));
            }
        }

        // For each item, find the lowest BIN and flag anything below it by threshold
        for (Map.Entry<String, List<Models.BinAuction>> e : byItem.entrySet()) {
            List<Models.BinAuction> list = e.getValue();
            if (list.size() < 4) continue;

            Collections.sort(list, Comparator.comparingDouble(b -> b.price));
            double lowest = list.get(0).price;
            double secondLowest = list.size() > 1 ? list.get(1).price : lowest;
            double reference = robustReferencePrice(list);

            // If the cheapest is much cheaper than the next one, it's a snipe candidate
            if (secondLowest <= 0 || reference <= 0) continue;
            double gapPct = (reference - lowest) / reference * 100.0;
            if (gapPct < cfg.minBinFlipPercent) continue;

            Models.BinAuction target = list.get(0);
            double profit = reference - lowest;
            if (profit < cfg.minProfitPerFlip) continue;

            out.add(new FlipOpportunity(
                    FlipOpportunity.Type.BIN_SNIPE,
                    target.itemName,
                    target.itemId,
                    lowest, secondLowest, profit, gapPct, 1, list.size()));
        }

        Collections.sort(out, Comparator.comparingDouble(FlipOpportunity::totalProfit).reversed());
        if (out.size() > cfg.maxResults) {
            return new ArrayList<FlipOpportunity>(out.subList(0, cfg.maxResults));
        }
        return out;
    }

    private static double robustReferencePrice(List<Models.BinAuction> sorted) {
        int from = 1; // skip cheapest to reduce outlier impact
        int to = Math.min(sorted.size(), 6); // next up to 5 comps
        if (to - from <= 0) return 0.0;
        List<Double> comps = new ArrayList<Double>();
        for (int i = from; i < to; i++) comps.add(sorted.get(i).price);
        Collections.sort(comps);
        int n = comps.size();
        if (n % 2 == 1) return comps.get(n / 2);
        return (comps.get((n / 2) - 1) + comps.get(n / 2)) / 2.0;
    }
}

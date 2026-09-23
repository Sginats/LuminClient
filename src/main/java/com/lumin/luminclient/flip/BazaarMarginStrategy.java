package com.lumin.luminclient.flip;

import com.lumin.luminclient.api.Models;
import com.lumin.luminclient.config.LuminConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Bazaar margin flipping.
 *
 * A bazaar margin flip is: place a BUY ORDER slightly above the current best buy
 * order, get filled, then place a SELL OFFER slightly below the current best sell
 * offer. The "spread" between best sell and best buy is your gross margin.
 *
 * This mod only computes and displays these margins. It does not place the orders.
 */
public final class BazaarMarginStrategy {

    // Bazaar tax (sell offer fee) charged by Hypixel. 1% for most players.
    private static final double BAZAAR_SELL_TAX = 0.01;

    public List<FlipOpportunity> findFlips(Models.BazaarSnapshot snapshot, LuminConfig cfg) {
        List<FlipOpportunity> out = new ArrayList<FlipOpportunity>();
        if (snapshot == null || snapshot.products == null) return out;

        for (Models.BazaarProduct p : snapshot.products.values()) {
            double buy = p.bestSellPrice;   // you pay the ask
            double sell = p.bestBuyPrice;   // you hit the bid
            if (buy <= 0 || sell <= 0 || sell <= buy) continue;

            double gross = sell - buy;
            double net = gross - (sell * BAZAAR_SELL_TAX);
            double pct = net / buy * 100.0;

            if (pct < cfg.minMarginPercent) continue;
            if (net < cfg.minProfitPerFlip && pct < cfg.minMarginPercent * 2) continue;

            // How many units does our budget cover, capped by visible liquidity
            long budgetUnits = (long) Math.floor(cfg.maxBudget / buy);
            long liquidityCap = Math.max(1, Math.min(p.buyVolume, p.sellVolume) / 20); // <=5% of book
            long units = Math.max(1, Math.min(budgetUnits, liquidityCap));
            if (units <= 0) continue;

            out.add(new FlipOpportunity(
                    FlipOpportunity.Type.BAZAAR_MARGIN,
                    prettyName(p.productId),
                    buy, sell, net, pct, units, Math.min(p.buyVolume, p.sellVolume)));
        }

        Collections.sort(out, new Comparator<FlipOpportunity>() {
            @Override
            public int compare(FlipOpportunity a, FlipOpportunity b) {
                return Double.compare(b.totalProfit(), a.totalProfit());
            }
        });

        if (out.size() > cfg.maxResults) {
            return new ArrayList<FlipOpportunity>(out.subList(0, cfg.maxResults));
        }
        return out;
    }

    static String prettyName(String productId) {
        if (productId == null) return "?";
        String[] parts = productId.replace(':', '_').split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) sb.append(part.substring(1).toLowerCase());
            sb.append(' ');
        }
        return sb.toString().trim();
    }
}

package com.lumin.luminclient.flip;

import com.lumin.luminclient.config.LuminConfig;
import com.lumin.luminclient.market.BazaarProduct;
import com.lumin.luminclient.market.BazaarSnapshot;
import com.lumin.luminclient.market.Execution;
import com.lumin.luminclient.market.FreshnessState;
import com.lumin.luminclient.market.MarketFeeModel;
import com.lumin.luminclient.market.MarketOpportunity;
import com.lumin.luminclient.market.OpportunityMetrics;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Evaluates executable, fee-inclusive Bazaar margins without performing network or execution work. */
public final class BazaarMarginStrategy {
    private static final Duration MAX_SNAPSHOT_AGE = Duration.ofMinutes(1);

    private final MarketFeeModel feeModel;
    private final Clock clock;

    public BazaarMarginStrategy() {
        this(MarketFeeModel.standardBazaar(), Clock.systemUTC());
    }

    BazaarMarginStrategy(MarketFeeModel feeModel, Clock clock) {
        this.feeModel = feeModel;
        this.clock = clock;
    }

    public List<FlipOpportunity> findFlips(BazaarSnapshot snapshot, LuminConfig cfg) {
        List<FlipOpportunity> opportunities = new ArrayList<FlipOpportunity>();
        if (snapshot == null) {
            return opportunities;
        }

        FreshnessState freshness = FreshnessState.assess(
                snapshot.lastUpdated(), Instant.now(clock), MAX_SNAPSHOT_AGE, List.of("bazaar snapshot"));
        if (!freshness.fresh()) {
            return opportunities;
        }

        long budget = wholeCoins(cfg.maxBudget);
        for (BazaarProduct product : snapshot.products().values()) {
            long liquidityCap = Math.min(product.orderBook().executablePurchaseQuantity(),
                    product.orderBook().executableLiquidationQuantity()) / 20L;
            long quantity = largestAffordableQuantity(product, Math.min(liquidityCap, budget), budget);
            if (quantity <= 0) {
                continue;
            }

            Execution acquisition = product.orderBook().executePurchase(quantity);
            Execution liquidation = product.orderBook().executeLiquidation(quantity);
            if (!acquisition.fullyFilled() || !liquidation.fullyFilled()) {
                continue;
            }
            long fees = feeModel.liquidationFee(liquidation.totalCoins());
            long grossProfit = saturatedDifference(liquidation.totalCoins(), acquisition.totalCoins());
            long netProfit = saturatedDifference(grossProfit, fees);
            double netPercent = acquisition.totalCoins() == 0 ? 0.0 : (double) netProfit / acquisition.totalCoins() * 100.0;
            long slippage = slippage(product, quantity, acquisition, liquidation);
            OpportunityMetrics metrics = new OpportunityMetrics(quantity, acquisition.totalCoins(),
                    liquidation.totalCoins(), grossProfit, fees, slippage, netProfit, liquidityCap, netPercent);
            MarketOpportunity opportunity = new MarketOpportunity(product.productId(), quantity, metrics, freshness,
                    List.of("fees included", "order-book execution", "slippage=" + slippage));

            if (!opportunity.isProfitable() || netPercent < cfg.minMarginPercent
                    || (netProfit < wholeCoins(cfg.minProfitPerFlip) && netPercent < cfg.minMarginPercent * 2.0)) {
                continue;
            }
            opportunities.add(new FlipOpportunity(FlipOpportunity.Type.BAZAAR_MARGIN, prettyName(product.productId()),
                    product.productId(), acquisition.totalCoins() / quantity, liquidation.totalCoins() / quantity,
                    netProfit / quantity, netPercent, quantity, liquidityCap, opportunity));
        }

        opportunities.sort(Comparator.comparingLong(FlipOpportunity::totalProfit).reversed());
        return opportunities.size() > cfg.maxResults
                ? new ArrayList<FlipOpportunity>(opportunities.subList(0, cfg.maxResults)) : opportunities;
    }

    private static long largestAffordableQuantity(BazaarProduct product, long maxQuantity, long budget) {
        if (maxQuantity <= 0 || budget <= 0) {
            return 0L;
        }
        long low = 1L;
        long high = maxQuantity;
        long best = 0L;
        while (low <= high) {
            long mid = low + (high - low) / 2;
            Execution execution = product.orderBook().executePurchase(mid);
            if (execution.fullyFilled() && execution.totalCoins() <= budget) {
                best = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return best;
    }

    private static long slippage(BazaarProduct product, long quantity, Execution acquisition, Execution liquidation) {
        long bestAsk = product.orderBook().sellLevels().isEmpty() ? 0L : product.orderBook().sellLevels().get(0).pricePerUnit();
        long bestBid = product.orderBook().buyLevels().isEmpty() ? 0L : product.orderBook().buyLevels().get(0).pricePerUnit();
        long purchaseSlippage = Math.max(0L, saturatedDifference(acquisition.totalCoins(), saturatedMultiply(bestAsk, quantity)));
        long liquidationSlippage = Math.max(0L, saturatedDifference(saturatedMultiply(bestBid, quantity), liquidation.totalCoins()));
        return saturatedAdd(purchaseSlippage, liquidationSlippage);
    }

    private static long wholeCoins(double value) {
        if (!Double.isFinite(value) || value <= 0.0) {
            return 0L;
        }
        return value >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) Math.floor(value);
    }

    private static long saturatedMultiply(long left, long right) {
        return left != 0 && right > Long.MAX_VALUE / left ? Long.MAX_VALUE : left * right;
    }

    private static long saturatedAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    private static long saturatedDifference(long left, long right) {
        if (right > 0 && left < Long.MIN_VALUE + right) {
            return Long.MIN_VALUE;
        }
        return left - right;
    }

    static String prettyName(String productId) {
        if (productId == null) return "?";
        String[] parts = productId.replace(':', '_').split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase()).append(' ');
            }
        }
        return sb.toString().trim();
    }
}

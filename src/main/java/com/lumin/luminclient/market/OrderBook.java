package com.lumin.luminclient.market;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Immutable Bazaar book. Buy levels are sorted highest-first and sell levels lowest-first.
 * Purchase execution consumes sell levels; liquidation execution consumes buy levels.
 */
public record OrderBook(List<OrderLevel> buyLevels, List<OrderLevel> sellLevels) {
    public OrderBook {
        buyLevels = sortedCopy(buyLevels, Comparator.comparingLong(OrderLevel::pricePerUnit).reversed());
        sellLevels = sortedCopy(sellLevels, Comparator.comparingLong(OrderLevel::pricePerUnit));
    }

    public Execution executePurchase(long quantity) {
        return execute(sellLevels, quantity);
    }

    public Execution executeLiquidation(long quantity) {
        return execute(buyLevels, quantity);
    }

    public long executablePurchaseQuantity() {
        return totalQuantity(sellLevels);
    }

    public long executableLiquidationQuantity() {
        return totalQuantity(buyLevels);
    }

    private static Execution execute(List<OrderLevel> levels, long requestedQuantity) {
        if (requestedQuantity <= 0) {
            return new Execution(Math.max(0L, requestedQuantity), 0L, 0L);
        }
        long remaining = requestedQuantity;
        long filled = 0L;
        long total = 0L;
        for (OrderLevel level : levels) {
            if (remaining == 0) {
                break;
            }
            long used = Math.min(remaining, level.quantity());
            total = saturatedAdd(total, saturatedMultiply(level.pricePerUnit(), used));
            filled = saturatedAdd(filled, used);
            remaining -= used;
        }
        return new Execution(requestedQuantity, filled, total);
    }

    private static List<OrderLevel> sortedCopy(List<OrderLevel> levels, Comparator<OrderLevel> comparator) {
        List<OrderLevel> valid = new ArrayList<OrderLevel>();
        if (levels != null) {
            valid.addAll(levels);
        }
        valid.sort(comparator);
        return List.copyOf(valid);
    }

    private static long totalQuantity(List<OrderLevel> levels) {
        long total = 0L;
        for (OrderLevel level : levels) {
            total = saturatedAdd(total, level.quantity());
        }
        return total;
    }

    static long saturatedAdd(long left, long right) {
        return Long.MAX_VALUE - left < right ? Long.MAX_VALUE : left + right;
    }

    static long saturatedMultiply(long left, long right) {
        return left != 0 && right > Long.MAX_VALUE / left ? Long.MAX_VALUE : left * right;
    }
}

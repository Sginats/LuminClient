package com.lumin.luminclient.market;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderBookTest {

    @Test
    void executesWeightedAcquisitionAndLiquidationAcrossLevels() {
        OrderBook book = new OrderBook(List.of(new OrderLevel(130, 2), new OrderLevel(120, 3)),
                List.of(new OrderLevel(100, 2), new OrderLevel(110, 3)));

        Execution acquisition = book.executePurchase(4);
        Execution liquidation = book.executeLiquidation(4);

        assertEquals(420L, acquisition.totalCoins());
        assertEquals(500L, liquidation.totalCoins());
        assertEquals(105.0, acquisition.averagePricePerUnit());
        assertTrue(acquisition.fullyFilled());
    }

    @Test
    void reportsInsufficientDepthAndHandlesEmptyOrInvalidRequests() {
        OrderBook book = new OrderBook(List.of(), List.of(new OrderLevel(100, 2)));
        Execution incomplete = book.executePurchase(3);

        assertFalse(incomplete.fullyFilled());
        assertEquals(2L, incomplete.filledQuantity());
        assertEquals(1L, incomplete.unfilledQuantity());
        assertEquals(0L, book.executeLiquidation(1).filledQuantity());
        assertEquals(0L, book.executePurchase(-4).requestedQuantity());
    }

    @Test
    void saturatesOverflowingTotals() {
        OrderBook book = new OrderBook(List.of(), List.of(new OrderLevel(Long.MAX_VALUE, 2)));
        assertEquals(Long.MAX_VALUE, book.executePurchase(2).totalCoins());
    }
}

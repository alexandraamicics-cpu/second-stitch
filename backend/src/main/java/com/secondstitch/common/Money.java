package com.secondstitch.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Small, pure helper functions for the money maths in the app.
 * No Spring, no database: this makes them easy to unit test (see MoneyTest).
 */
public final class Money {

    private Money() {
    }

    /**
     * Cost per wear = price paid / times worn.
     * An item you have never worn counts as worn once, so a ₱300 jacket
     * starts at ₱300 per wear and gets "cheaper" every time you wear it.
     */
    public static BigDecimal costPerWear(BigDecimal pricePaid, int wearCount) {
        if (pricePaid == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        int wears = Math.max(wearCount, 1);
        return pricePaid.divide(BigDecimal.valueOf(wears), 2, RoundingMode.HALF_UP);
    }

    /**
     * How much you saved compared with buying the item new.
     * Unknown retail price, or paying more than retail, counts as zero savings.
     */
    public static BigDecimal savings(BigDecimal pricePaid, BigDecimal estimatedRetail) {
        if (pricePaid == null || estimatedRetail == null) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal diff = estimatedRetail.subtract(pricePaid);
        return diff.signum() > 0 ? diff.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
    }
}

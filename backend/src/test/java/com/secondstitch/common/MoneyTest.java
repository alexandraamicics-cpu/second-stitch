package com.secondstitch.common;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain unit tests: no Spring, no database, they run in milliseconds.
 * These were written before Money.java (test-driven development):
 * write a failing test, make it pass, then tidy up.
 */
class MoneyTest {

    @Test
    void costPerWearDividesPriceByWears() {
        assertThat(Money.costPerWear(new BigDecimal("300"), 4)).isEqualByComparingTo("75.00");
    }

    @Test
    void neverWornItemCostsFullPricePerWear() {
        assertThat(Money.costPerWear(new BigDecimal("300"), 0)).isEqualByComparingTo("300.00");
    }

    @Test
    void costPerWearRoundsToCentavos() {
        assertThat(Money.costPerWear(new BigDecimal("100"), 3)).isEqualByComparingTo("33.33");
    }

    @Test
    void savingsIsRetailMinusPaid() {
        assertThat(Money.savings(new BigDecimal("350"), new BigDecimal("4290"))).isEqualByComparingTo("3940.00");
    }

    @Test
    void unknownRetailMeansNoSavings() {
        assertThat(Money.savings(new BigDecimal("350"), null)).isEqualByComparingTo("0");
    }

    @Test
    void payingMoreThanRetailIsNotNegativeSavings() {
        assertThat(Money.savings(new BigDecimal("500"), new BigDecimal("400"))).isEqualByComparingTo("0");
    }
}

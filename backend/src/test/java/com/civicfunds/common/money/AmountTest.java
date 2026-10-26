package com.civicfunds.common.money;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * These tests are the specification of CivicFunds' central safety rule:
 * dollar figures that measure different things are never combined.
 */
class AmountTest {

    @Test
    @DisplayName("adds two amounts that measure the same thing")
    void addsCompatibleAmounts() {
        Amount a = Amount.of("12000000.00", AmountClass.OBLIGATED);
        Amount b = Amount.of("4500000.00", AmountClass.OBLIGATED);

        assertThat(a.plus(b).value()).isEqualByComparingTo("16500000.00");
        assertThat(a.plus(b).amountClass()).isEqualTo(AmountClass.OBLIGATED);
    }

    @Test
    @DisplayName("refuses to sum an award ceiling into an outlay")
    void refusesToSumAwardedAndOutlayed() {
        Amount awarded = Amount.of("12000000.00", AmountClass.AWARDED);
        Amount outlayed = Amount.of("3200000.00", AmountClass.OUTLAYED);

        assertThatThrownBy(() -> awarded.plus(outlayed))
                .isInstanceOf(IncompatibleAmountException.class)
                .hasMessageContaining("do not measure the same thing");
    }

    @Test
    @DisplayName("refuses to sum a five-year programmed plan into an obligation")
    void refusesToSumProgrammedAndObligated() {
        Amount programmed = Amount.of("40000000.00", AmountClass.PROGRAMMED);
        Amount obligated = Amount.of("12000000.00", AmountClass.OBLIGATED);

        assertThatThrownBy(() -> programmed.plus(obligated))
                .isInstanceOf(IncompatibleAmountException.class);
    }

    @Test
    @DisplayName("refuses to sum amounts whose measurement the source never stated")
    void refusesToSumUnknownAmounts() {
        Amount a = Amount.of("100.00", AmountClass.UNKNOWN);
        Amount b = Amount.of("200.00", AmountClass.UNKNOWN);

        assertThatThrownBy(() -> a.plus(b))
                .isInstanceOf(IncompatibleAmountException.class)
                .hasMessageContaining("did not state what they measure");
    }

    @Test
    @DisplayName("rejects a mixed list rather than returning a misleading total")
    void refusesToSumMixedList() {
        List<Amount> mixed = List.of(
                Amount.of("12000000.00", AmountClass.OBLIGATED),
                Amount.of("4500000.00", AmountClass.PROGRAMMED),
                Amount.of("2800000.00", AmountClass.APPROPRIATED));

        assertFalse(Amount.isHomogeneous(mixed));
        assertThatThrownBy(() -> Amount.sum(mixed))
                .isInstanceOf(IncompatibleAmountException.class);
    }

    @Test
    @DisplayName("sums a homogeneous list")
    void sumsHomogeneousList() {
        List<Amount> same = List.of(
                Amount.of("1000.00", AmountClass.OBLIGATED),
                Amount.of("2000.00", AmountClass.OBLIGATED),
                Amount.of("3000.50", AmountClass.OBLIGATED));

        assertTrue(Amount.isHomogeneous(same));
        assertThat(Amount.sum(same).value()).isEqualByComparingTo("6000.50");
    }

    @Test
    @DisplayName("never renders a bare number")
    void alwaysDisplaysItsClass() {
        assertThat(Amount.of("12000000", AmountClass.OBLIGATED).display())
                .isEqualTo("$12,000,000.00 obligated");
    }

    @Test
    @DisplayName("rounds to cents on construction")
    void roundsToCents() {
        assertThat(Amount.of("2313357.754", AmountClass.AWARDED).value())
                .isEqualByComparingTo("2313357.75");
    }
}

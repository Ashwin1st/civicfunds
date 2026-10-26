package com.civicfunds.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * A dollar figure that knows what it measures.
 *
 * <p>Deliberately has no {@code plus(BigDecimal)} and no untyped constructor.
 * The only way to combine two Amounts is {@link #plus(Amount)}, which throws
 * if the classes differ. Summing an award ceiling into an outlay is a bug,
 * so the type system refuses to express it.
 */
public record Amount(BigDecimal value, AmountClass amountClass) {

    public Amount {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(amountClass, "amountClass");
        value = value.setScale(2, RoundingMode.HALF_UP);
    }

    public static Amount of(BigDecimal value, AmountClass amountClass) {
        return new Amount(value, amountClass);
    }

    public static Amount of(String value, AmountClass amountClass) {
        return new Amount(new BigDecimal(value), amountClass);
    }

    public static Amount zero(AmountClass amountClass) {
        return new Amount(BigDecimal.ZERO, amountClass);
    }

    /**
     * @throws IncompatibleAmountException if the two amounts measure different things.
     */
    public Amount plus(Amount other) {
        Objects.requireNonNull(other, "other");
        if (this.amountClass != other.amountClass) {
            throw new IncompatibleAmountException(this.amountClass, other.amountClass);
        }
        if (!this.amountClass.isSummable()) {
            throw new IncompatibleAmountException(
                    "Amounts of class " + amountClass
                    + " cannot be summed: the source did not state what they measure.");
        }
        return new Amount(this.value.add(other.value), this.amountClass);
    }

    /**
     * Sums a list that must be homogeneous.
     *
     * @throws IncompatibleAmountException if the list mixes classes.
     */
    public static Amount sum(List<Amount> amounts) {
        if (amounts == null || amounts.isEmpty()) {
            throw new IncompatibleAmountException("Cannot sum an empty list: no amount class to attribute the total to.");
        }
        Amount total = amounts.get(0);
        for (int i = 1; i < amounts.size(); i++) {
            total = total.plus(amounts.get(i));
        }
        return total;
    }

    /** True when every amount shares one summable class. */
    public static boolean isHomogeneous(List<Amount> amounts) {
        if (amounts == null || amounts.isEmpty()) {
            return false;
        }
        AmountClass first = amounts.get(0).amountClass();
        if (!first.isSummable()) {
            return false;
        }
        return amounts.stream().allMatch(a -> a.amountClass() == first);
    }

    /** e.g. "$12,000,000.00 obligated" - never a bare number. */
    public String display() {
        return String.format("$%,.2f %s", value, amountClass.label());
    }
}

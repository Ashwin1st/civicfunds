package com.civicfunds.common.money;

/**
 * Thrown when code attempts to combine dollar figures that measure
 * different things. This is a programming error, not a user error.
 */
public class IncompatibleAmountException extends RuntimeException {

    public IncompatibleAmountException(AmountClass left, AmountClass right) {
        super("Refusing to combine " + left.label() + " and " + right.label()
              + " amounts: they do not measure the same thing.");
    }

    public IncompatibleAmountException(String message) {
        super(message);
    }
}

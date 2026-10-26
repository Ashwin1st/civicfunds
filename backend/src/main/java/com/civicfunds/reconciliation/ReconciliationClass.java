package com.civicfunds.reconciliation;

/**
 * How two funding records inside the same project relate to each other.
 *
 * <p>The default is {@link #UNKNOWN_RELATIONSHIP}, and the default blocks
 * summation. Nothing is added together until something proves it should be.
 */
public enum ReconciliationClass {

    /** Independent dollars. Safe to add, within a single AmountClass. */
    UNIQUE_FUNDING(true),

    /** Federal money flowing through a state or local body. The same dollars, counted once at origin. */
    PASS_THROUGH(false),

    /** Genuinely additional non-federal dollars. Safe to add. */
    MATCHING_FUNDS(true),

    /** A re-report of prime award dollars. Never additive - subaward totals can exceed the prime. */
    SUBAWARD(false),

    /** The same award observed in two systems. Keep one. */
    DUPLICATE_RECORD(false),

    /** The same dollars at different lifecycle stages (awarded then obligated then outlayed). */
    SUCCESSIVE_MEASUREMENT(false),

    /** We do not know. Blocks any total. */
    UNKNOWN_RELATIONSHIP(false);

    private final boolean additive;

    ReconciliationClass(boolean additive) {
        this.additive = additive;
    }

    /** Whether records in this relationship may contribute to a displayed total. */
    public boolean isAdditive() {
        return additive;
    }
}

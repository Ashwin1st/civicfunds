package com.civicfunds.common.money;

/**
 * What a dollar figure actually measures.
 *
 * <p>This is the most important type in CivicFunds. Government systems publish
 * numbers that look alike and mean different things: a five-year programmed
 * plan figure, a binding obligation, cash actually disbursed, and an enacted
 * appropriation are not the same quantity and must never be added together.
 *
 * <p>Every persisted amount carries one of these, NOT NULL, and
 * {@link Amount#plus(Amount)} refuses to combine two different classes.
 */
public enum AmountClass {

    /** Enacted budget authority (city council ordinance, state GAA line). */
    APPROPRIATED("appropriated", "Enacted budget authority - money a legislature or council has authorized to be spent."),

    /** Planned in a multi-year capital or transportation program (TIP, CIP). */
    PROGRAMMED("programmed", "Planned in a multi-year program. A plan, not a commitment - out-year figures change."),

    /** Award ceiling; base and all options. */
    AWARDED("awarded", "The ceiling of an award. Not all of it is necessarily committed or spent."),

    /** A binding commitment has been made. */
    OBLIGATED("obligated", "A binding commitment. The government has promised this money to a recipient."),

    /** Cash actually disbursed. */
    OUTLAYED("outlayed", "Cash actually paid out."),

    /** Non-federal match named in an agreement. */
    MATCH_REQUIRED("match required", "A non-federal contribution required as a condition of an award."),

    /** Planning estimate published by the source. */
    ESTIMATED("estimated", "A planning estimate published by the source agency."),

    /** The source did not say, and we will not guess. */
    UNKNOWN("unknown", "The source record did not identify what this figure measures.");

    private final String label;
    private final String definition;

    AmountClass(String label, String definition) {
        this.label = label;
        this.definition = definition;
    }

    /** Short label rendered next to every figure in the UI. */
    public String label() {
        return label;
    }

    /** Plain-English tooltip text. Served from /api/glossary/terms. */
    public String definition() {
        return definition;
    }

    /**
     * Whether figures of this class may ever participate in a sum.
     * UNKNOWN never can - if the source did not say what a number measures,
     * CivicFunds does not add it to anything.
     */
    public boolean isSummable() {
        return this != UNKNOWN;
    }
}

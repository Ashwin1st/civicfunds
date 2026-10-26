package com.civicfunds.search;

/**
 * How a search result was found.
 *
 * <p>Carried on every hit and shown in the interface, because "how did you
 * find this?" is a question most search boxes refuse to answer. The three
 * values are not interchangeable: an identifier match is a lookup, a keyword
 * match is a shared word, and a fuzzy match is a resemblance.
 */
public enum MatchKind {

    /** Exact match on a government identifier. A join, not a guess. */
    IDENTIFIER("Exact lookup on a government identifier. No scoring involved."),

    /** Full-text match: the words really are in the record. */
    KEYWORD("Full-text match — these words appear in the record itself."),

    /** Trigram resemblance: close spelling, not the same words. */
    FUZZY("Close spelling rather than an exact word match — pg_trgm similarity.");

    private final String explanation;

    MatchKind(String explanation) {
        this.explanation = explanation;
    }

    public String explanation() {
        return explanation;
    }
}

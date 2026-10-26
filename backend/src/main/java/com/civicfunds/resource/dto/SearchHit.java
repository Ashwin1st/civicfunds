package com.civicfunds.resource.dto;

/**
 * One search result.
 *
 * <p>Note what is <em>not</em> here: a score, a rank, or a figure detached
 * from its measurement class. {@code amount} and {@code amountClass} always
 * travel together, because a number without its class is not a fact this
 * application is willing to publish — not in the database, not over the wire,
 * not on screen.
 *
 * <p>{@code matchKind} and {@code matchNote} say how the record was found.
 * They are part of the result, not debug output.
 */
public record SearchHit(

        Long id,

        /** IDENTIFIER, KEYWORD or FUZZY. */
        String matchKind,

        /** Plain-English explanation of that match kind. */
        String matchNote,

        String title,
        String sourceSystem,
        String governmentLevel,

        /** Null when the source published no figure. Never zero as a stand-in. */
        String amount,

        /** Never null. APPROPRIATED, PROGRAMMED, AWARDED, OBLIGATED, OUTLAYED… */
        String amountClass,

        String recipient,
        String fundingAgency,

        /** The source's own project key, where it has one. */
        String externalKey,

        String place,
        String councilDistrict,

        String sourceUrl,
        String retrievedAt
) {
}

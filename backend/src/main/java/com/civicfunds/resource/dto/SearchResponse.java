package com.civicfunds.resource.dto;

import java.util.List;
import java.util.Map;

/**
 * A search response.
 *
 * <p>{@code totalShown} is always false and {@code whyNoTotal} always says
 * why. That is not defensive padding: the refusal is the product's central
 * claim, so it belongs in the API and not only in the interface. A client
 * that wanted to sum {@code hits} would have to ignore a field that tells it
 * not to.
 */
public record SearchResponse(

        String query,

        int total,

        /** How many hits came from each rung: IDENTIFIER, KEYWORD, FUZZY. */
        Map<String, Integer> byMatchKind,

        /** Distinct measurement classes among the hits. More than one means no sum is possible at all. */
        List<String> amountClassesPresent,

        /** Always false. */
        boolean totalShown,

        String whyNoTotal,

        List<SearchHit> hits
) {
}

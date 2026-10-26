package com.civicfunds.search;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Decides whether what someone typed is a government identifier.
 *
 * <p>This matters because an identifier is the only kind of query CivicFunds
 * can answer without guessing. An FM number, a FAIN, a PIID or a contract
 * number is a key: the record either carries it or it does not, and the
 * answer is a lookup at confidence 100 with no scoring involved. Everything
 * else is text similarity, which is a judgement.
 *
 * <p>Deliberately a plain class with no Spring and no database, so the rule
 * can be unit-tested by hand. The tests in {@code IdentifierQueryTest} are
 * checkable on paper.
 *
 * <p>People write identifiers with the scheme attached — "FM 4374372",
 * "CFDA 14.267", "contract E2T38" — so the scheme word is stripped before
 * matching. What remains must be a single token, at least four characters,
 * and contain at least one digit. "Lem Turner" is two words and no digits,
 * so it is text, not a key.
 */
public final class IdentifierQuery {

    /** Scheme words people type in front of the number itself. */
    private static final Set<String> SCHEME_WORDS = Set.of(
            "FM", "FPID", "CFDA", "FAIN", "PIID", "WPITEM",
            "CONTRACT", "AWARD", "GRANT", "ORDINANCE", "NO", "NO.", "#");

    private static final int MIN_LENGTH = 4;

    private IdentifierQuery() {
    }

    /**
     * @return the bare identifier if this query looks like one, otherwise empty.
     */
    public static Optional<String> normalize(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String[] tokens = raw.trim().toUpperCase(Locale.ROOT).split("\\s+");
        if (tokens.length == 0 || tokens[0].isEmpty()) {
            return Optional.empty();
        }

        // Drop leading scheme words: "CFDA 14.267" and "GRANT NO. 123456" both
        // reduce to the number. Never drop the last token, or "FM" alone would
        // normalize to the empty string.
        int start = 0;
        while (start < tokens.length - 1 && SCHEME_WORDS.contains(tokens[start])) {
            start++;
        }
        if (tokens.length - start != 1) {
            return Optional.empty();   // still a phrase, so it is text
        }

        String candidate = tokens[start];
        // A trailing separator is punctuation, not part of the key.
        candidate = candidate.replaceAll("[,;:]+$", "");

        if (candidate.length() < MIN_LENGTH || !hasDigit(candidate)) {
            return Optional.empty();
        }
        return Optional.of(candidate);
    }

    /** True when this query should be tried against the identifier columns first. */
    public static boolean looksLikeIdentifier(String raw) {
        return normalize(raw).isPresent();
    }

    private static boolean hasDigit(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isDigit(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}

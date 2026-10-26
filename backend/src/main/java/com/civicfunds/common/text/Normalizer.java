package com.civicfunds.common.text;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Text normalisation for matching only.
 *
 * <p>Nothing here ever overwrites a source value. The normalised form is stored
 * alongside the original so a record always shows what the government actually
 * published while still being matchable.
 */
public final class Normalizer {

    private Normalizer() { }

    private static final Map<String, String> ABBREVIATIONS = Map.ofEntries(
            Map.entry("DEPT", "DEPARTMENT"), Map.entry("DEPTS", "DEPARTMENTS"),
            Map.entry("AUTH", "AUTHORITY"), Map.entry("CNTY", "COUNTY"),
            Map.entry("CO", "COUNTY"), Map.entry("MGMT", "MANAGEMENT"),
            Map.entry("SVCS", "SERVICES"), Map.entry("SVC", "SERVICE"),
            Map.entry("ADMIN", "ADMINISTRATION"), Map.entry("COMM", "COMMISSION"),
            Map.entry("TRANS", "TRANSPORTATION"), Map.entry("DIST", "DISTRICT"),
            Map.entry("UNIV", "UNIVERSITY"), Map.entry("NATL", "NATIONAL"),
            Map.entry("FL", "FLORIDA"), Map.entry("JAX", "JACKSONVILLE"),
            Map.entry("ASSOC", "ASSOCIATION"), Map.entry("FDN", "FOUNDATION"));

    private static final Set<String> LEGAL_SUFFIXES =
            Set.of("INC", "LLC", "LLP", "LP", "CORP", "CORPORATION", "CO",
                   "COMPANY", "LTD", "PA", "PLLC", "PC", "NA");

    private static final Set<String> STOP_WORDS = Set.of("THE", "OF", "AND", "A", "AN", "FOR");

    /**
     * Uppercase, strip punctuation, expand abbreviations, drop legal suffixes
     * and stop words, collapse whitespace. Applied to organisation names.
     */
    public static String organisationName(String raw) {
        if (raw == null) return null;
        String cleaned = raw.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.isEmpty()) return "";

        String[] tokens = cleaned.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            token = ABBREVIATIONS.getOrDefault(token, token);
            boolean lastToken = (i == tokens.length - 1);
            if (lastToken && LEGAL_SUFFIXES.contains(token)) continue;
            if (STOP_WORDS.contains(token)) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(token);
        }
        String result = sb.toString().trim();
        return result.isEmpty() ? cleaned : result;
    }

    /**
     * Trims and collapses whitespace, returning null for blanks.
     *
     * <p>Named for the reason it exists: FDOT's ArcGIS layers return
     * fixed-width, space-padded strings - a value comes back as
     * {@code "N/A                           "}, which silently breaks equality
     * and inequality filters alike. Every adapter runs source strings through
     * this before storing them.
     */
    public static String trimToNull(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim().replaceAll("\\s+", " ");
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Title-cases a shouty government string for display, keeping acronyms. */
    public static String forDisplay(String raw) {
        String value = trimToNull(raw);
        if (value == null) return null;
        if (!value.equals(value.toUpperCase(Locale.ROOT))) return value;
        StringBuilder sb = new StringBuilder(value.length());
        for (String word : value.split(" ")) {
            if (sb.length() > 0) sb.append(' ');
            if (word.length() <= 3 && !STOP_WORDS.contains(word)) {
                sb.append(word);
            } else {
                sb.append(word.charAt(0))
                  .append(word.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }
}

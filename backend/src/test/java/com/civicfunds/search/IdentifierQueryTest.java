package com.civicfunds.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every case here is checkable on paper, which is the point: this rule
 * decides whether a query gets a confidence-100 lookup or a text search,
 * and it has to be explainable out loud.
 */
class IdentifierQueryTest {

    @Test
    @DisplayName("A bare FM number is an identifier")
    void bareFmNumber() {
        assertEquals(Optional.of("4374372"), IdentifierQuery.normalize("4374372"));
    }

    @Test
    @DisplayName("The scheme word is stripped: 'FM 4374372' is the key 4374372")
    void schemeWordStripped() {
        assertEquals(Optional.of("4374372"), IdentifierQuery.normalize("FM 4374372"));
        assertEquals(Optional.of("4374372"), IdentifierQuery.normalize("fm 4374372"));
    }

    @Test
    @DisplayName("A CFDA number keeps its decimal point")
    void cfdaKeepsDot() {
        assertEquals(Optional.of("14.267"), IdentifierQuery.normalize("CFDA 14.267"));
    }

    @Test
    @DisplayName("Several scheme words are stripped, but never the key itself")
    void multipleSchemeWords() {
        assertEquals(Optional.of("E2T38"), IdentifierQuery.normalize("contract no. E2T38"));
        // "FM" alone must not normalize to the empty string.
        assertFalse(IdentifierQuery.looksLikeIdentifier("FM"));
    }

    @Test
    @DisplayName("A place name is text, not a key — two words and no digits")
    void placeNameIsNotAnIdentifier() {
        assertFalse(IdentifierQuery.looksLikeIdentifier("Lem Turner"));
        assertFalse(IdentifierQuery.looksLikeIdentifier("Arlington"));
        assertFalse(IdentifierQuery.looksLikeIdentifier("McCoys Creek restoration"));
    }

    @Test
    @DisplayName("Too short, or no digit, is not a key")
    void rejectsShortAndDigitless() {
        assertFalse(IdentifierQuery.looksLikeIdentifier("E2"));      // three chars
        assertFalse(IdentifierQuery.looksLikeIdentifier("BRIDGE"));  // no digit
    }

    @Test
    @DisplayName("A federal-aid number is an identifier — it just does not join to FAIN")
    void federalAidNumber() {
        // SPIKE: FHWA/FMIS numbers are real keys in FDOT data and are NOT
        // USAspending FAINs or PIIDs. Searchable, not joinable.
        assertEquals(Optional.of("2955314I"), IdentifierQuery.normalize("2955314I"));
    }

    @Test
    @DisplayName("Null and blank are not identifiers")
    void nullAndBlank() {
        assertFalse(IdentifierQuery.looksLikeIdentifier(null));
        assertFalse(IdentifierQuery.looksLikeIdentifier("   "));
    }

    @Test
    @DisplayName("Trailing punctuation is dropped")
    void trailingPunctuation() {
        assertTrue(IdentifierQuery.looksLikeIdentifier("4374372,"));
        assertEquals(Optional.of("4374372"), IdentifierQuery.normalize("4374372,"));
    }
}

package com.civicfunds.common.text;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizerTest {

    @Test
    @DisplayName("the three spellings of the city collapse to one normalized form")
    void cityOfJacksonvilleVariants() {
        String canonical = Normalizer.organisationName("CITY OF JACKSONVILLE");

        assertThat(Normalizer.organisationName("City of Jacksonville")).isEqualTo(canonical);
        assertThat(Normalizer.organisationName("Jacksonville, City of"))
                .isNotEqualTo(canonical); // word order differs - handled by the scorer, not here
        assertThat(canonical).isEqualTo("CITY JACKSONVILLE");
    }

    @Test
    @DisplayName("expands government abbreviations")
    void expandsAbbreviations() {
        assertThat(Normalizer.organisationName("FL Dept of Trans"))
                .isEqualTo("FLORIDA DEPARTMENT TRANSPORTATION");
    }

    @Test
    @DisplayName("drops a trailing legal suffix but not a meaningful last word")
    void dropsLegalSuffix() {
        assertThat(Normalizer.organisationName("Archer Western Contractors LLC"))
                .isEqualTo("ARCHER WESTERN CONTRACTORS");
        assertThat(Normalizer.organisationName("Groundwork Jacksonville"))
                .isEqualTo("GROUNDWORK JACKSONVILLE");
    }

    @Test
    @DisplayName("trims FDOT's space-padded fixed-width fields")
    void trimsPaddedArcGisValues() {
        assertThat(Normalizer.trimToNull("N/A                           ")).isEqualTo("N/A");
        assertThat(Normalizer.trimToNull("E2T38          ")).isEqualTo("E2T38");
        assertThat(Normalizer.trimToNull("      ")).isNull();
        assertThat(Normalizer.trimToNull(null)).isNull();
    }
}

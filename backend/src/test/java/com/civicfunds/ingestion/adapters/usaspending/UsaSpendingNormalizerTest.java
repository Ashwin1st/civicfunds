package com.civicfunds.ingestion.adapters.usaspending;

import com.civicfunds.common.GovernmentLevel;
import com.civicfunds.common.money.AmountClass;
import com.civicfunds.ingestion.IngestionBatch;
import com.civicfunds.ingestion.RawFundingRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Normalisation is where a federal figure gets its measurement class. A wrong
 * mapping here does not throw - it quietly mislabels every federal number in
 * the application, so these assertions are the guard rail.
 */
class UsaSpendingNormalizerTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final UsaSpendingNormalizer normalizer = new UsaSpendingNormalizer(mapper);

    private RawFundingRecord raw(String json) {
        return new RawFundingRecord(
                new IngestionBatch("USASPENDING", "test"),
                "USASPENDING", "ASST_NON_TEST_1",
                "https://www.usaspending.gov/award/ASST_NON_TEST_1",
                Instant.parse("2026-09-08T12:00:00Z"), json, "x".repeat(64));
    }

    private static final String ASSISTANCE =
            "{\"Award ID\":\"FAIN123\",\"Recipient Name\":\"CITY OF JACKSONVILLE\","
            + "\"Funding Agency\":\"Department of Transportation\","
            + "\"Award Amount\":\"12000000.00\",\"Total Outlays\":\"3200000.00\","
            + "\"Description\":\"ARLINGTON STORMWATER RESILIENCE IMPROVEMENT PROJECT\","
            + "\"CFDA Number\":\"20.205\",\"Start Date\":\"2024-01-15\",\"End Date\":\"2027-06-30\","
            + "\"Base Obligation Date\":\"2024-01-15\","
            + "\"Primary Place of Performance\":{\"city_name\":\"JACKSONVILLE\","
            + "\"county_name\":\"DUVAL\",\"county_code\":\"031\",\"state_code\":\"FL\","
            + "\"zip5\":\"32202\",\"congressional_code\":\"04\"}}";

    @Test
    @DisplayName("Award Amount is obligated money, and Total Outlays is cash paid")
    void mapsAmountClasses() {
        var n = normalizer.normalize(raw(ASSISTANCE));

        assertThat(n.getAmount()).isEqualByComparingTo("12000000.00");
        assertThat(n.getAmountClass()).isEqualTo(AmountClass.OBLIGATED);
        assertThat(n.getSecondaryAmount()).isEqualByComparingTo("3200000.00");
        assertThat(n.getSecondaryAmountClass()).isEqualTo(AmountClass.OUTLAYED);
        // The two must never be the same class, or the reconciler would add them.
        assertThat(n.getAmountClass()).isNotEqualTo(n.getSecondaryAmountClass());
    }

    @Test
    @DisplayName("reads county and district out of the Primary Place of Performance object")
    void readsNestedPlaceOfPerformance() {
        var n = normalizer.normalize(raw(ASSISTANCE));

        assertThat(n.getCounty()).isEqualTo("DUVAL");
        assertThat(n.getCity()).isEqualTo("JACKSONVILLE");
        assertThat(n.getState()).isEqualTo("FL");
        assertThat(n.getZip5()).isEqualTo("32202");
        assertThat(n.getCongressionalDistrict()).isEqualTo("04");
        assertThat(n.getGovernmentLevel()).isEqualTo(GovernmentLevel.FEDERAL);
    }

    @Test
    @DisplayName("no coordinates are invented - this API never returns them")
    void doesNotInventCoordinates() {
        var n = normalizer.normalize(raw(ASSISTANCE));
        assertThat(n.getLatitude()).isNull();
        assertThat(n.getLongitude()).isNull();
    }

    @Test
    @DisplayName("an absent amount becomes UNKNOWN, which blocks every total")
    void missingAmountIsUnknownNotZero() {
        var n = normalizer.normalize(raw("{\"Award ID\":\"X\",\"Description\":\"a project\"}"));

        assertThat(n.getAmount()).isNull();
        assertThat(n.getAmountClass()).isEqualTo(AmountClass.UNKNOWN);
        assertThat(n.getAmountClass().isSummable()).isFalse();
    }

    @Test
    @DisplayName("a loan face value is never mapped as obligated money")
    void loansAreNotGrants() {
        var n = normalizer.normalize(raw("{\"Award ID\":\"L1\",\"Loan Value\":\"5000000.00\"}"));

        assertThat(n.getAmountClass()).isEqualTo(AmountClass.ESTIMATED);
        assertThat(n.getAmountClass()).isNotEqualTo(AmountClass.OBLIGATED);
    }

    @Test
    @DisplayName("an administrative code is not promoted to a project title")
    void doesNotInventTitlesFromCodes() {
        assertThat(UsaSpendingNormalizer.weakTitle("IGF::OT::IGF")).isNull();
        assertThat(UsaSpendingNormalizer.weakTitle("   ")).isNull();
        assertThat(UsaSpendingNormalizer.weakTitle("Arlington drainage improvements"))
                .isEqualTo("Arlington drainage improvements");
    }

    @Test
    @DisplayName("the assistance listing becomes the external key when present")
    void usesCfdaAsExternalKey() {
        assertThat(normalizer.normalize(raw(ASSISTANCE)).getExternalKey()).isEqualTo("CFDA:20.205");
    }
}

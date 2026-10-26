package com.civicfunds.ingestion.adapters.usaspending;

import com.civicfunds.ingestion.IngestionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Request-shape tests. These do not call the network: they assert that the body
 * CivicFunds builds matches the documented contract, which is where the
 * expensive mistakes live.
 */
class UsaSpendingAdapterTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final UsaSpendingProperties props =
            new UsaSpendingProperties(null, null, null, 0, 0, null, null, 3, null);
    private final UsaSpendingAdapter adapter =
            new UsaSpendingAdapter(null, props, mapper);
    private final IngestionRequest request =
            new IngestionRequest(LocalDate.of(2023, 10, 1), LocalDate.of(2026, 9, 1), 0);

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> locationsOf(AwardTypeGroup group) {
        Map<String, Object> body = adapter.body(group, request, 1);
        Map<String, Object> filters = (Map<String, Object>) body.get("filters");
        return (List<Map<String, Object>>) filters.get("place_of_performance_locations");
    }

    @Test
    @DisplayName("all three counties of Florida's 4th District are requested, not just Duval")
    void coversTheWholeDistrict() {
        List<Map<String, Object>> locations = locationsOf(AwardTypeGroup.GRANTS);

        assertThat(locations).hasSize(3);
        assertThat(locations).allSatisfy(loc -> assertThat(loc)
                .containsEntry("country", "USA")
                .containsEntry("state", "FL"));
        assertThat(locations).extracting(loc -> loc.get("county"))
                .containsExactlyInAnyOrder("031", "019", "089");   // Duval, Clay, Nassau
    }

    @Test
    @DisplayName("county filter uses the three-digit code, never the five-digit FIPS")
    void countyFilterIsThreeDigits() {
        // 12031 returns zero rows silently rather than erroring. Guard against a
        // well-meaning "fix" that swaps in the five-digit form.
        assertThat(locationsOf(AwardTypeGroup.CONTRACTS))
                .extracting(loc -> (String) loc.get("county"))
                .allSatisfy(code -> assertThat(code).hasSize(3))
                .doesNotContain("12031", "12019", "12089");
    }

    @Test
    @DisplayName("a five-digit county code is rejected at construction rather than failing silently")
    void rejectsFiveDigitCountyCode() {
        assertThatThrownBy(() -> new UsaSpendingProperties(
                null, null, List.of("12031"), 0, 0, null, null, 3, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("three digits");
    }

    @Test
    @DisplayName("county names are available for the batch descriptor and coverage page")
    void namesTheCounties() {
        assertThat(props.countyNames()).isEqualTo("Duval, Clay, Nassau");
    }

    @Test
    @DisplayName("page size never exceeds the documented maximum of 100")
    void pageSizeCappedAtApiMaximum() {
        UsaSpendingProperties oversized =
                new UsaSpendingProperties(null, null, null, 500, 0, null, null, 3, null);
        assertThat(oversized.pageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("contract and assistance award types are never mixed in one request")
    void awardTypesAreNotMixed() {
        for (AwardTypeGroup group : AwardTypeGroup.values()) {
            Map<String, Object> body = adapter.body(group, request, 1);
            @SuppressWarnings("unchecked")
            Map<String, Object> filters = (Map<String, Object>) body.get("filters");
            @SuppressWarnings("unchecked")
            List<String> codes = (List<String>) filters.get("award_type_codes");

            boolean hasContract = codes.stream().anyMatch(c -> List.of("A","B","C","D").contains(c));
            boolean hasAssistance = codes.stream().anyMatch(c -> c.matches("0[2-9]|1[01]|-1"));
            assertThat(hasContract && hasAssistance)
                    .as("group %s mixes contract and assistance codes", group)
                    .isFalse();
        }
    }

    @Test
    @DisplayName("the sort field is always one of the requested fields")
    void sortFieldIsRequested() {
        for (AwardTypeGroup group : AwardTypeGroup.values()) {
            assertThat(group.fields()).contains(group.sortField());
        }
    }

    @Test
    @DisplayName("Primary Place of Performance is requested, because county lives inside it")
    void requestsPlaceOfPerformanceObject() {
        assertThat(AwardTypeGroup.BASE_FIELDS)
                .contains("Primary Place of Performance", "generated_internal_id");
    }

    @Test
    @DisplayName("county-rollup and PII-suppressed rows are rejected, not mapped as projects")
    void rejectsAggregateAndRedactedRows() throws Exception {
        var aggregate = mapper.readTree("{\"Recipient Name\":\"MULTIPLE RECIPIENTS\"}");
        var redacted  = mapper.readTree("{\"Recipient Name\":\"REDACTED DUE TO PII\"}");
        var real      = mapper.readTree("{\"Recipient Name\":\"CITY OF JACKSONVILLE\"}");

        assertThat(adapter.isNonProjectRecord(aggregate)).isTrue();
        assertThat(adapter.isNonProjectRecord(redacted)).isTrue();
        assertThat(adapter.isNonProjectRecord(real)).isFalse();
    }
}

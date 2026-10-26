package com.civicfunds.ingestion.adapters.usaspending;

import com.civicfunds.ingestion.FundingSourceAdapter;
import com.civicfunds.ingestion.IngestionException;
import com.civicfunds.ingestion.IngestionRequest;
import com.civicfunds.ingestion.RawRecordCandidate;
import com.civicfunds.ingestion.SourceSystem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Federal awards with a place of performance in Duval County, from the official
 * USAspending v2 API.
 *
 * <p>Three things here are easy to get wrong and are handled deliberately:
 *
 * <ol>
 *   <li><b>The county code is three digits.</b> The filter takes {@code "031"};
 *       bulk-download output uses the five-digit FIPS {@code 12031}. Passing the
 *       five-digit form silently returns zero rows rather than an error.</li>
 *   <li><b>Award types cannot be mixed.</b> Type-specific fields are validated
 *       against the requested types, so each {@link AwardTypeGroup} is a
 *       separate call and the results are unioned.</li>
 *   <li><b>Aggregate and redacted rows are not projects.</b> USAspending
 *       publishes county-level rollups with the recipient replaced by
 *       {@code MULTIPLE RECIPIENTS}, and PII-suppressed rows reading
 *       {@code REDACTED DUE TO PII}. Both are guaranteed to carry a county code,
 *       so a naive county filter over-selects exactly these. Mapping them would
 *       put pseudo-projects on the map, so they are dropped here and counted.</li>
 * </ol>
 *
 * <p>Subawards are deliberately not fetched by this adapter. They are a
 * re-report of prime award dollars and can exceed the prime award total, so
 * they belong in a separate flagged table rather than mixed into this stream.
 */
@Component
public class UsaSpendingAdapter implements FundingSourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(UsaSpendingAdapter.class);

    private static final String SEARCH_PATH = "/api/v2/search/spending_by_award/";
    private static final String AWARD_UI = "https://www.usaspending.gov/award/";

    /** Recipient values that mark a row as an aggregate or suppressed record. */
    private static final List<String> NON_PROJECT_RECIPIENTS =
            List.of("MULTIPLE RECIPIENTS", "REDACTED DUE TO PII");

    private final UsaSpendingClient client;
    private final UsaSpendingProperties properties;
    private final ObjectMapper objectMapper;

    public UsaSpendingAdapter(UsaSpendingClient client,
                              UsaSpendingProperties properties,
                              ObjectMapper objectMapper) {
        this.client = client;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public SourceSystem sourceSystem() {
        return SourceSystem.USASPENDING;
    }

    @Override
    public String describeRequest(IngestionRequest request) {
        return "USAspending spending_by_award; place of performance "
                + properties.stateCode() + " counties " + properties.countyNames()
                + " (Florida's 4th Congressional District); action dates "
                + request.fromDate() + " to " + request.toDate()
                + (request.isCapped() ? "; capped at " + request.maxRecords() + " records" : "");
    }

    @Override
    public List<RawRecordCandidate> fetch(IngestionRequest request) {
        List<RawRecordCandidate> all = new ArrayList<>();
        int skippedNonProject = 0;

        // A cap is a sampling instruction, not a truncation point. Spending the
        // whole budget on the first group returns nothing but defense contracts,
        // sorted by amount descending - and since only assistance awards carry a
        // CFDA number, that silently strips every deterministic identifier out of
        // the sample, which in turn makes identifier matching untestable. Each
        // group gets an equal share.
        int quota = request.isCapped()
                ? Math.max(1, request.maxRecords() / AwardTypeGroup.values().length)
                : Integer.MAX_VALUE;

        List<String> failedGroups = new ArrayList<>();

        for (AwardTypeGroup group : AwardTypeGroup.values()) {
            int page = 1, taken = 0;
            boolean hasNext = true;

            try {
                while (hasNext && page <= properties.maxPages() && taken < quota) {
                    JsonNode response = client.post(SEARCH_PATH, body(group, request, page));
                    JsonNode results = response.path("results");
                    if (!results.isArray() || results.isEmpty()) break;

                    Instant retrievedAt = Instant.now();
                    for (JsonNode row : results) {
                        if (taken >= quota) break;
                        if (isNonProjectRecord(row)) {
                            skippedNonProject++;
                            continue;
                        }
                        String id = row.path("generated_internal_id").asText(null);
                        if (id == null || id.isBlank()) {
                            // Without the stable key we cannot dedupe or hydrate it later.
                            continue;
                        }
                        all.add(new RawRecordCandidate(
                                id, AWARD_UI + id, row.toString(), retrievedAt));
                        taken++;
                    }

                    hasNext = response.path("page_metadata").path("hasNext").asBoolean(false);
                    page++;
                }
            } catch (RuntimeException e) {
                // One group the API will not serve should not discard the other
                // five. A partial ingestion that says which part is missing is
                // worth more than an empty one, and the batch record keeps the
                // reason.
                failedGroups.add(group.name());
                log.warn("USAspending group {} failed after {} records; continuing: {}",
                        group, taken, e.toString());
                continue;
            }
            log.info("USAspending {}: {} records ({} total)", group, taken, all.size());
        }

        if (failedGroups.size() == AwardTypeGroup.values().length) {
            throw new IngestionException(SourceSystem.USASPENDING,
                    "Every award-type group failed: " + String.join(", ", failedGroups));
        }
        if (!failedGroups.isEmpty()) {
            log.warn("Ingestion is incomplete: no records fetched for {}",
                    String.join(", ", failedGroups));
        }

        if (skippedNonProject > 0) {
            log.info("Skipped {} aggregate or PII-suppressed rows - these are county rollups, "
                     + "not individual projects", skippedNonProject);
        }
        return all;
    }

    /**
     * Aggregate (record_type 1) and PII-suppressed (record_type 3) rows are
     * identified by their recipient name, because spending_by_award does not
     * expose record_type as a returnable field or a filter.
     */
    boolean isNonProjectRecord(JsonNode row) {
        String recipient = row.path("Recipient Name").asText("");
        String upper = recipient.toUpperCase(Locale.ROOT);
        return NON_PROJECT_RECIPIENTS.stream().anyMatch(upper::contains);
    }

    Map<String, Object> body(AwardTypeGroup group, IngestionRequest request, int page) {
        Map<String, Object> timePeriod = new LinkedHashMap<>();
        timePeriod.put("start_date", request.fromDate().toString());
        timePeriod.put("end_date", request.toDate().toString());

        // One location entry per county. The API ORs them, so all three counties
        // of Florida's 4th District come back from a single call.
        List<Map<String, Object>> locations = new ArrayList<>();
        for (String county : properties.countyCodes()) {
            Map<String, Object> location = new LinkedHashMap<>();
            location.put("country", "USA");
            location.put("state", properties.stateCode());
            location.put("county", county);
            locations.add(location);
        }

        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("award_type_codes", group.codes());
        filters.put("time_period", List.of(timePeriod));
        filters.put("place_of_performance_locations", locations);

        Map<String, Object> body = new HashMap<>();
        body.put("filters", filters);
        body.put("fields", group.fields());
        body.put("limit", properties.pageSize());
        body.put("page", page);
        body.put("sort", group.sortField());
        body.put("order", "desc");
        body.put("subawards", false);
        return body;
    }

    /** Exposed for tests: the request body as JSON. */
    String bodyAsJson(AwardTypeGroup group, IngestionRequest request, int page) {
        try {
            return objectMapper.writeValueAsString(body(group, request, page));
        } catch (Exception e) {
            throw new IngestionException(SourceSystem.USASPENDING, "Could not serialise request", e);
        }
    }
}

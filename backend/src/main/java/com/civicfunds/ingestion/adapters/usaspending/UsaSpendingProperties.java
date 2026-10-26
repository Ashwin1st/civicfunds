package com.civicfunds.ingestion.adapters.usaspending;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * USAspending is keyless and documents no rate limit, but there is no promise
 * that an edge layer does not throttle. Concurrency stays at one and the
 * defaults below are deliberately unhurried.
 */
@ConfigurationProperties(prefix = "civicfunds.usaspending")
public record UsaSpendingProperties(
        String baseUrl,
        String stateCode,
        List<String> countyCodes,
        int pageSize,
        int maxPages,
        Duration connectTimeout,
        Duration readTimeout,
        int maxRetries,
        Duration retryBackoff) {

    /**
     * Florida's 4th Congressional District is three whole counties, so covering
     * only Duval would leave two thirds of the district invisible.
     *
     * <p>These are the THREE-digit codes the place-of-performance filter expects.
     * The five-digit FIPS forms (12031 / 12019 / 12089) appear in bulk-download
     * output and silently return zero rows if passed here.
     */
    public static final Map<String, String> DISTRICT_COUNTIES;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("031", "Duval");
        m.put("019", "Clay");
        m.put("089", "Nassau");
        DISTRICT_COUNTIES = Map.copyOf(m);
    }

    public UsaSpendingProperties {
        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://api.usaspending.gov";
        if (stateCode == null || stateCode.isBlank()) stateCode = "FL";
        if (countyCodes == null || countyCodes.isEmpty()) {
            countyCodes = List.of("031", "019", "089");
        }
        countyCodes = countyCodes.stream().map(String::trim).toList();
        for (String code : countyCodes) {
            if (code.length() != 3) {
                throw new IllegalArgumentException(
                        "County code '" + code + "' must be three digits. The place-of-performance "
                        + "filter takes 031, not the five-digit FIPS 12031 — the five-digit form "
                        + "returns zero rows without raising an error.");
            }
        }
        if (pageSize <= 0 || pageSize > 100) pageSize = 100;   // API maximum is 100
        if (maxPages <= 0) maxPages = 200;                      // 20,000 records; cap is 50,000 by offset
        if (connectTimeout == null) connectTimeout = Duration.ofSeconds(10);
        if (readTimeout == null) readTimeout = Duration.ofSeconds(60);
        if (maxRetries < 0) maxRetries = 3;
        if (retryBackoff == null) retryBackoff = Duration.ofSeconds(2);
    }

    /** e.g. "Duval, Clay, Nassau" — for the batch descriptor and the coverage page. */
    public String countyNames() {
        return countyCodes.stream()
                .map(c -> DISTRICT_COUNTIES.getOrDefault(c, c))
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }
}

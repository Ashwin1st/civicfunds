package com.civicfunds.resource.dto;

import com.civicfunds.common.GovernmentLevel;

import java.util.List;
import java.util.Map;

/**
 * Backs the "About this data" link. Being explicit about what is and is not
 * indexed is cheap to build and is the most credible thing on the site.
 */
public record CoverageResponse(
        long rawRecords,
        long normalizedRecords,
        long recordsWithDeterministicKey,
        List<String> sourceSystems,
        Map<GovernmentLevel, Long> recordsByGovernmentLevel) {
}

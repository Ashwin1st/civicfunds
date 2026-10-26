package com.civicfunds.resource.dto;

import java.util.List;

/**
 * What is indexed, counted every way except the one that would be wrong.
 *
 * <p>Every number in here is a count of RECORDS. There is no total dollar
 * figure and no field that could hold one. That is not an omission to be
 * filled in later: the records carry several different measurement classes,
 * and a sum across them would describe nothing that exists. The absence is
 * the feature, so it is enforced by the shape of this type rather than left
 * to whoever writes the next endpoint.
 */
public record OverviewResponse(
        long records,
        long sourceSystems,
        long withGovernmentKey,
        int measurementClassCount,
        List<Bucket> byGovernmentLevel,
        List<Bucket> byMeasurementClass,
        List<Bucket> bySourceSystem,
        List<Bucket> byCategory,
        List<Bucket> byCity) {

    public record Bucket(String label, long count) {
    }
}

package com.civicfunds.ingestion;

import java.time.Instant;

/**
 * One record as a source returned it, before CivicFunds has decided whether it
 * is new. The orchestrator turns this into a {@link RawFundingRecord} - or
 * discards it as a duplicate - by checksum.
 *
 * @param sourceRecordId the source's own stable identifier for this row
 * @param sourceUrl      a URL a human can follow back to the record
 * @param payloadJson    the untouched JSON the source returned for this row
 * @param retrievedAt    when the fetch happened
 */
public record RawRecordCandidate(String sourceRecordId, String sourceUrl,
                                 String payloadJson, Instant retrievedAt) {

    public RawRecordCandidate {
        if (sourceRecordId == null || sourceRecordId.isBlank()) {
            throw new IllegalArgumentException("sourceRecordId is required");
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new IllegalArgumentException("payloadJson is required");
        }
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "sourceUrl is required - a record with no traceable origin is not ingestible");
        }
        if (retrievedAt == null) {
            throw new IllegalArgumentException("retrievedAt is required");
        }
    }
}

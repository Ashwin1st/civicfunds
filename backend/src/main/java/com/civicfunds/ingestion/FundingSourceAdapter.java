package com.civicfunds.ingestion;

import java.util.List;

/**
 * One government data source.
 *
 * <p>An adapter's only job is to return what the source actually said, with a
 * URL and a timestamp attached. It does not normalize, geocode, categorise or
 * interpret - those are separate stages that run against the stored raw
 * records, so they can be re-run after a bug fix without re-fetching.
 *
 * <p>Implementations must be side-effect free with respect to the database.
 */
public interface FundingSourceAdapter {

    SourceSystem sourceSystem();

    /**
     * A human-readable description of the request, stored on the batch so the
     * coverage page can say exactly what was asked for.
     */
    String describeRequest(IngestionRequest request);

    /**
     * Fetch matching records. Implementations handle their own pagination,
     * timeouts and retries, and throw {@link IngestionException} on failure.
     */
    List<RawRecordCandidate> fetch(IngestionRequest request);
}

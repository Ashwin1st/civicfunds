package com.civicfunds.ingestion;

import java.time.LocalDate;

/**
 * What to ask a source for. Deliberately small: every adapter targets Duval
 * County, so geography is not a parameter - it is the point of the system.
 *
 * @param fromDate   inclusive start of the action-date window
 * @param toDate     inclusive end of the action-date window
 * @param maxRecords safety valve so a mis-typed date range cannot pull a
 *                   million rows; 0 means "no cap".
 */
public record IngestionRequest(LocalDate fromDate, LocalDate toDate, int maxRecords) {

    public IngestionRequest {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("fromDate and toDate are required");
        }
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate must not be before fromDate");
        }
        if (maxRecords < 0) {
            throw new IllegalArgumentException("maxRecords must not be negative");
        }
    }

    public boolean isCapped() {
        return maxRecords > 0;
    }
}

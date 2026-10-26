package com.civicfunds.resource.dto;

import com.civicfunds.ingestion.IngestionBatch;

import java.time.Instant;

public record IngestionBatchResponse(
        Long id,
        String sourceSystem,
        String status,
        Instant startedAt,
        Instant finishedAt,
        int recordsReceived,
        int recordsStored,
        int recordsDuplicate,
        int recordsFailed,
        String requestDescriptor,
        String errorMessage) {

    public static IngestionBatchResponse from(IngestionBatch b) {
        return new IngestionBatchResponse(
                b.getId(), b.getSourceSystem(), b.getStatus().name(),
                b.getStartedAt(), b.getFinishedAt(),
                b.getRecordsReceived(), b.getRecordsStored(),
                b.getRecordsDuplicate(), b.getRecordsFailed(),
                b.getRequestDescriptor(), b.getErrorMessage());
    }
}

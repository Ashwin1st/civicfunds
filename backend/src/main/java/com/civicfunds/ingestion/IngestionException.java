package com.civicfunds.ingestion;

/** A source could not be read. Carries the source system for the batch record. */
public class IngestionException extends RuntimeException {

    private final SourceSystem sourceSystem;

    public IngestionException(SourceSystem sourceSystem, String message) {
        super(message);
        this.sourceSystem = sourceSystem;
    }

    public IngestionException(SourceSystem sourceSystem, String message, Throwable cause) {
        super(message, cause);
        this.sourceSystem = sourceSystem;
    }

    public SourceSystem sourceSystem() {
        return sourceSystem;
    }
}

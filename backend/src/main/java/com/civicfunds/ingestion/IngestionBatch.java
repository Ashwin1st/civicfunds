package com.civicfunds.ingestion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "ingestion_batch")
@Getter
@Setter
@NoArgsConstructor
public class IngestionBatch {

    public enum Status { RUNNING, COMPLETED, FAILED, PARTIAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_system", nullable = false, length = 64)
    private String sourceSystem;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Status status = Status.RUNNING;

    @Column(name = "records_received", nullable = false)
    private int recordsReceived;

    @Column(name = "records_stored", nullable = false)
    private int recordsStored;

    @Column(name = "records_duplicate", nullable = false)
    private int recordsDuplicate;

    @Column(name = "records_failed", nullable = false)
    private int recordsFailed;

    /** Human-readable description of what was requested, for the coverage page. */
    @Column(name = "request_descriptor", columnDefinition = "text")
    private String requestDescriptor;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    public IngestionBatch(String sourceSystem, String requestDescriptor) {
        this.sourceSystem = sourceSystem;
        this.requestDescriptor = requestDescriptor;
    }
}

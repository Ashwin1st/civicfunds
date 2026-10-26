package com.civicfunds.ingestion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * An untouched copy of exactly what a government source returned.
 *
 * <p>Append-only, enforced by a database trigger rather than by convention.
 * Nothing in this table is ever updated or deleted: re-ingesting a changed
 * record inserts a new row alongside the old one. That is what makes it
 * possible to re-run normalization, entity resolution and matching from
 * scratch after fixing a bug, without re-fetching anything, and to show a
 * user the original record behind any figure the app displays.
 *
 * <p>Deliberately has no setters.
 */
@Entity
@Table(name = "raw_funding_record")
@Getter
@NoArgsConstructor
public class RawFundingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingestion_batch_id", nullable = false, updatable = false)
    private IngestionBatch ingestionBatch;

    @Column(name = "source_system", nullable = false, updatable = false, length = 64)
    private String sourceSystem;

    @Column(name = "source_record_id", nullable = false, updatable = false, length = 255)
    private String sourceRecordId;

    @Column(name = "source_url", nullable = false, updatable = false, columnDefinition = "text")
    private String sourceUrl;

    @Column(name = "retrieved_at", nullable = false, updatable = false)
    private Instant retrievedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false, updatable = false, columnDefinition = "jsonb")
    private String rawPayload;

    /** SHA-256 of the canonical JSON payload. Makes re-ingestion idempotent. */
    @Column(nullable = false, updatable = false, length = 64)
    private String checksum;

    public RawFundingRecord(IngestionBatch batch, String sourceSystem, String sourceRecordId,
                            String sourceUrl, Instant retrievedAt, String rawPayload, String checksum) {
        this.ingestionBatch = batch;
        this.sourceSystem = sourceSystem;
        this.sourceRecordId = sourceRecordId;
        this.sourceUrl = sourceUrl;
        this.retrievedAt = retrievedAt;
        this.rawPayload = rawPayload;
        this.checksum = checksum;
    }
}

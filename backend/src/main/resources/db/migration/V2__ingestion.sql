-- ============================================================
-- Ingestion: raw records are APPEND-ONLY and are never mutated.
-- Re-ingesting a changed record inserts a new row; the old one
-- stays so any downstream conclusion can be re-derived later.
-- ============================================================

CREATE TABLE ingestion_batch (
    id                  BIGSERIAL PRIMARY KEY,
    source_system       VARCHAR(64)  NOT NULL,
    started_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    finished_at         TIMESTAMPTZ,
    status              VARCHAR(24)  NOT NULL DEFAULT 'RUNNING',
    records_received    INTEGER      NOT NULL DEFAULT 0,
    records_stored      INTEGER      NOT NULL DEFAULT 0,
    records_duplicate   INTEGER      NOT NULL DEFAULT 0,
    records_failed      INTEGER      NOT NULL DEFAULT 0,
    request_descriptor  TEXT,
    error_message       TEXT,
    CONSTRAINT ck_batch_status
        CHECK (status IN ('RUNNING','COMPLETED','FAILED','PARTIAL'))
);

CREATE INDEX ix_batch_source ON ingestion_batch (source_system, started_at DESC);

CREATE TABLE raw_funding_record (
    id                  BIGSERIAL PRIMARY KEY,
    ingestion_batch_id  BIGINT       NOT NULL REFERENCES ingestion_batch (id),
    source_system       VARCHAR(64)  NOT NULL,
    source_record_id    VARCHAR(255) NOT NULL,
    source_url          TEXT         NOT NULL,
    retrieved_at        TIMESTAMPTZ  NOT NULL,
    raw_payload         JSONB        NOT NULL,
    checksum            CHAR(64)     NOT NULL,
    CONSTRAINT uq_raw_identity UNIQUE (source_system, source_record_id, checksum)
);

CREATE INDEX ix_raw_source   ON raw_funding_record (source_system, source_record_id);
CREATE INDEX ix_raw_batch    ON raw_funding_record (ingestion_batch_id);

-- Enforce append-only at the database, not by convention.
CREATE OR REPLACE FUNCTION raw_funding_record_is_immutable()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION
        'raw_funding_record is append-only; % is not permitted (id=%)',
        TG_OP, OLD.id;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_raw_funding_record_immutable
    BEFORE UPDATE OR DELETE ON raw_funding_record
    FOR EACH ROW EXECUTE FUNCTION raw_funding_record_is_immutable();

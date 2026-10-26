-- ============================================================
-- The normalized record. Note that EVERY monetary column is
-- paired with a NOT NULL amount_class. A number without its
-- measurement class is not storable in this schema, by design:
-- award != obligation != outlay != appropriation != programmed.
-- ============================================================

CREATE TABLE normalized_funding_record (
    id                     BIGSERIAL PRIMARY KEY,
    raw_record_id          BIGINT       NOT NULL REFERENCES raw_funding_record (id),
    source_system          VARCHAR(64)  NOT NULL,
    government_level       VARCHAR(16)  NOT NULL,

    award_id               VARCHAR(255),
    external_key           VARCHAR(255),
    award_type             VARCHAR(120),

    amount                 NUMERIC(18,2),
    amount_class           VARCHAR(24)  NOT NULL,
    secondary_amount       NUMERIC(18,2),
    secondary_amount_class VARCHAR(24),

    funding_agency_id      BIGINT REFERENCES organization (id),
    recipient_id           BIGINT REFERENCES organization (id),
    funding_agency_raw     VARCHAR(300),
    recipient_raw          VARCHAR(300),

    project_title          VARCHAR(500),
    project_description    TEXT,
    category               VARCHAR(64),
    status                 VARCHAR(32),

    start_date             DATE,
    end_date               DATE,
    award_date             DATE,
    fiscal_year            VARCHAR(16),

    city                   VARCHAR(120),
    county                 VARCHAR(120),
    state                  VARCHAR(2),
    zip5                   VARCHAR(5),
    latitude               DOUBLE PRECISION,
    longitude              DOUBLE PRECISION,
    council_district       VARCHAR(8),
    congressional_district VARCHAR(8),

    source_url             TEXT         NOT NULL,
    retrieved_at           TIMESTAMPTZ  NOT NULL,
    last_updated           TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_norm_level
        CHECK (government_level IN ('FEDERAL','STATE','REGIONAL','LOCAL','OTHER')),
    CONSTRAINT ck_norm_amount_class
        CHECK (amount_class IN ('APPROPRIATED','PROGRAMMED','AWARDED','OBLIGATED',
                                'OUTLAYED','MATCH_REQUIRED','ESTIMATED','UNKNOWN')),
    CONSTRAINT ck_norm_secondary_class
        CHECK (secondary_amount_class IS NULL OR secondary_amount_class IN
               ('APPROPRIATED','PROGRAMMED','AWARDED','OBLIGATED',
                'OUTLAYED','MATCH_REQUIRED','ESTIMATED','UNKNOWN')),
    -- A secondary amount without its class is not representable.
    CONSTRAINT ck_norm_secondary_paired
        CHECK ((secondary_amount IS NULL) = (secondary_amount_class IS NULL)),
    CONSTRAINT uq_norm_raw UNIQUE (raw_record_id)
);

CREATE INDEX ix_norm_level        ON normalized_funding_record (government_level);
CREATE INDEX ix_norm_category     ON normalized_funding_record (category);
CREATE INDEX ix_norm_recipient    ON normalized_funding_record (recipient_id);
CREATE INDEX ix_norm_external_key ON normalized_funding_record (external_key);
CREATE INDEX ix_norm_district     ON normalized_funding_record (council_district);
CREATE INDEX ix_norm_title_trgm
    ON normalized_funding_record USING gin (project_title gin_trgm_ops);

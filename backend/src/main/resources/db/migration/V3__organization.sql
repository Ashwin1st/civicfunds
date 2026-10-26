-- Agencies and recipients are one table with a role, not two.
CREATE TABLE organization (
    id                BIGSERIAL PRIMARY KEY,
    canonical_name    VARCHAR(300) NOT NULL,
    normalized_name   VARCHAR(300) NOT NULL,
    government_level  VARCHAR(16)  NOT NULL,
    uei               VARCHAR(12),
    domain            VARCHAR(255),
    city              VARCHAR(120),
    state             VARCHAR(2),
    zip5              VARCHAR(5),
    seeded            BOOLEAN      NOT NULL DEFAULT FALSE,
    review_status     VARCHAR(16)  NOT NULL DEFAULT 'CONFIRMED',
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_org_level
        CHECK (government_level IN ('FEDERAL','STATE','REGIONAL','LOCAL','PRIVATE','NONPROFIT','OTHER')),
    CONSTRAINT ck_org_review
        CHECK (review_status IN ('CONFIRMED','CANDIDATE','REJECTED')),
    CONSTRAINT uq_org_uei UNIQUE (uei)
);

CREATE INDEX ix_org_normalized_trgm
    ON organization USING gin (normalized_name gin_trgm_ops);
CREATE INDEX ix_org_level ON organization (government_level);

CREATE TABLE organization_alias (
    id               BIGSERIAL PRIMARY KEY,
    organization_id  BIGINT       NOT NULL REFERENCES organization (id) ON DELETE CASCADE,
    alias            VARCHAR(300) NOT NULL,
    normalized_alias VARCHAR(300) NOT NULL,
    source_system    VARCHAR(64),
    CONSTRAINT uq_alias UNIQUE (normalized_alias, organization_id)
);

CREATE INDEX ix_alias_normalized ON organization_alias (normalized_alias);

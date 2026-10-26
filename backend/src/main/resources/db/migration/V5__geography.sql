-- Boundary geometry is deliberately NOT mapped in JPA. PostGIS handles
-- point-in-polygon via native queries; Hibernate Spatial is not worth
-- the dependency for the handful of spatial operations this app needs.
CREATE TABLE boundary (
    id            BIGSERIAL PRIMARY KEY,
    boundary_type VARCHAR(32)  NOT NULL,
    code          VARCHAR(32)  NOT NULL,
    name          VARCHAR(200) NOT NULL,
    source_url    TEXT,
    retrieved_at  TIMESTAMPTZ,
    geom          geometry(MultiPolygon, 4326) NOT NULL,
    CONSTRAINT ck_boundary_type
        CHECK (boundary_type IN ('COUNCIL_DISTRICT','CONGRESSIONAL_DISTRICT',
                                 'ZIP','NEIGHBORHOOD','COUNTY','CITY')),
    CONSTRAINT uq_boundary UNIQUE (boundary_type, code)
);

CREATE INDEX ix_boundary_geom ON boundary USING gist (geom);

-- Geocode once per unique address, ever. The pipeline reruns often.
CREATE TABLE geocode_cache (
    normalized_address VARCHAR(500) PRIMARY KEY,
    latitude           DOUBLE PRECISION,
    longitude          DOUBLE PRECISION,
    match_quality      VARCHAR(32),
    provider           VARCHAR(64)  NOT NULL,
    retrieved_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

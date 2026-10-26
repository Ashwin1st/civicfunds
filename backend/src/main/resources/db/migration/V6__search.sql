-- ============================================================
-- V6 — search.
--
-- Step one of the product flow, and the first thing a resident
-- touches. Three rungs, tried in order by FundingSearchService:
--
--   1. identifier  — an FM number, FAIN, PIID or contract number
--                    matched exactly. A lookup, not a guess.
--   2. full text   — weighted tsvector over title, parties, place.
--   3. trigram     — pg_trgm, for misspellings and abbreviations.
--
-- The weights matter: a hit in the project title (A) outranks a hit
-- in the recipient name (B), which outranks a city name (C), which
-- outranks the description (D). Without that, searching "Jacksonville"
-- returns every record in the index in arbitrary order.
--
-- search_vector is a STORED generated column rather than a trigger.
-- to_tsvector(regconfig, text) is IMMUTABLE when the config is given
-- explicitly, which is what makes that legal — the one-argument form
-- is not, because it depends on a session setting.
-- ============================================================

ALTER TABLE normalized_funding_record
    ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(project_title,       '')), 'A') ||
        setweight(to_tsvector('english', coalesce(recipient_raw,       '')), 'B') ||
        setweight(to_tsvector('english', coalesce(funding_agency_raw,  '')), 'B') ||
        setweight(to_tsvector('english', coalesce(city,                '')), 'C') ||
        setweight(to_tsvector('english', coalesce(county,              '')), 'C') ||
        setweight(to_tsvector('english', coalesce(project_description, '')), 'D')
    ) STORED;

CREATE INDEX ix_norm_search ON normalized_funding_record USING gin (search_vector);

-- Trigram indexes for rung 3. A title index already exists (V4); the
-- parties need one too, because "Archer Western Contractors LLC" is
-- the kind of thing people half-remember.
CREATE INDEX ix_norm_recipient_trgm
    ON normalized_funding_record USING gin (recipient_raw gin_trgm_ops);
CREATE INDEX ix_norm_agency_trgm
    ON normalized_funding_record USING gin (funding_agency_raw gin_trgm_ops);

-- Rung 1 is case-insensitive exact matching, so the indexes have to be
-- on the same expression the query uses or they will not be used.
CREATE INDEX ix_norm_external_key_upper
    ON normalized_funding_record (upper(external_key));
CREATE INDEX ix_norm_award_id_upper
    ON normalized_funding_record (upper(award_id));

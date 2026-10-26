-- V2 declared the digest column as CHAR(64). Hibernate maps String(64) to
-- VARCHAR(64), and schema validation rejects the mismatch on startup.
--
-- VARCHAR is the right type regardless: a SHA-256 hex digest is fixed-length
-- by construction, so CHAR's blank-padding semantics add nothing and its
-- comparison rules are a trap waiting for whoever writes the next query.
ALTER TABLE raw_funding_record ALTER COLUMN checksum TYPE VARCHAR(64);

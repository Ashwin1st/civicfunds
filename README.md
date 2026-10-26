# CivicFunds

**Public Money. Real Projects. Clear Answers.**

Public funding records for Duval, Clay and Nassau counties, Florida — indexed,
searchable, and deliberately never added up.

Congressional App Challenge 2026.

---

## The problem

There is a bridge on Lem Turner Road in Jacksonville. Ask who is paying for it
and you will find:

| Source | Figure | What it measures |
|---|---|---|
| North Florida TPO work programme | **$81,322,496** | PROGRAMMED — scheduled in a plan revised every year |
| FDOT annual obligations report | **$651,879** | OBLIGATED — legally committed, for design only |
| The same TIP page | **$87,500,909** | total project cost, including money spent before FY2026 |

Three systems, three numbers, one bridge. None of them is wrong. They answer
different questions.

A few streets away, a crosswalk project reports **−$6,749**. That is a
de-obligation: money committed and handed back. It is a real, published,
negative figure.

Most public spending dashboards would add all of these together.

## What CivicFunds does

It indexes the records, keeps the provenance of every one, and **refuses to
produce a combined total** — showing instead what each figure measures and why
the sum would be meaningless.

- **Search** across 853 records by place, agency, project or government
  identifier, with a three-rung ladder: exact identifier → weighted full text →
  trigram similarity. Every result says which rung found it and why.
- **Measurement classes** on every figure: APPROPRIATED, PROGRAMMED, AWARDED,
  OBLIGATED, OUTLAYED, MATCH_REQUIRED, ESTIMATED, UNKNOWN.
- **Provenance** on every record: source system, source URL, retrieval date,
  and a checksum of the payload.
- **Honest gaps**: a record with no published amount says so instead of showing
  zero; a record with no published city is counted and labelled, not dropped.

## The rule, enforced in the type system

```java
Amount awarded  = Amount.of("12000000.00", AmountClass.AWARDED);
Amount outlayed = Amount.of("3200000.00",  AmountClass.OUTLAYED);

awarded.plus(outlayed);   // throws IncompatibleAmountException
```

Every persisted figure carries a `NOT NULL amount_class`. There is no way to
store a number without saying what it measures, and no way to add two numbers
that measure different things. The rule is not a convention anyone has to
remember — it is the only thing the type permits.

Raw records are **append-only**, enforced by a Postgres trigger rather than by
discipline:

```sql
CREATE TRIGGER trg_raw_funding_record_immutable
    BEFORE UPDATE OR DELETE ON raw_funding_record
    FOR EACH ROW EXECUTE FUNCTION raw_funding_record_is_immutable();
```

When a government changes a figure, the new version is stored beside the old
one. Nothing is overwritten, so any conclusion drawn from this data can be
re-derived later.

## Data sources

| Level | Source | How it is read | State |
|---|---|---|---|
| Federal | **USAspending.gov** | REST API, keyless, six award-type groups fetched separately | **850 records** |
| Federal | FHWA Eastern Federal Lands TIP | Read from the published PDF by a person | 1 record |
| State | North Florida TPO TIP | Read from the published PDF by a person | 1 record |
| State | FDOT annual obligations report | Read from the published PDF by a person | 1 record |
| Regional | North Florida TPO long range plan | No adapter | Not loaded |
| Local | Jacksonville budget and CIP | No adapter | Not loaded |
| Local | Duval County Public Schools capital plan | No adapter | Not loaded |

Records typed in by hand carry `MANUAL` in their source system and show it in
the interface. The figures and citations are real; the path they took is not
the same as an API fetch, and the application does not hide the difference.

**Rejected during ingestion:** 1,223 rows. USAspending publishes county-level
rollups with the recipient replaced by `MULTIPLE RECIPIENTS`, and
privacy-suppressed rows reading `REDACTED DUE TO PII`. Both always carry a
county code, so a naive county filter selects exactly those. They are not
projects, and mapping them would put pseudo-projects on a map.

## Scope: counties, not a district

CivicFunds indexes **three whole counties**. That is deliberately not the same
shape as a congressional district. Florida's 4th covers all of Clay, all of
Nassau, and only the part of Duval **west of the St. Johns River**; the rest of
Duval is Florida's 5th. So this index is a superset of FL-04 within these
counties and a subset of FL-05.

Every source filters on county, because county is what the federal and state
systems accept. Congressional district would have to be computed per project by
point-in-polygon — which is not built — so where a boundary has not been
tested, the application says `boundary test pending` rather than naming a
district.

## Stack

Java 21 · Spring Boot 3.3.5 · PostgreSQL 16 with `pg_trgm` (PostGIS optional) ·
Flyway · Maven · springdoc-openapi.

Front end is plain HTML, CSS and JavaScript served by Spring Boot from
`src/main/resources/static` — no framework, no build step. Modular monolith,
feature-oriented packages under `com.civicfunds`.

53 tests, JUnit 5 and MockMvc.

## Running it

```bash
docker compose up -d db          # PostgreSQL 16 + PostGIS

cd backend
mvn spring-boot:run
mvn test
```

Or point it at any PostgreSQL 16 by setting `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`. PostGIS is
optional — `V1__extensions.sql` creates it if available and carries on without
it; only the unused `boundary` geometry column depends on it.

| | |
|---|---|
| Application | http://localhost:8080/ |
| API docs | http://localhost:8080/swagger-ui.html |
| Coverage | http://localhost:8080/api/meta/coverage |

Loading data — two steps, deliberately:

```bash
curl -u admin:admin -X POST \
  "localhost:8080/api/admin/ingestion/usaspending?from=2024-10-01&to=2026-09-30&maxRecords=900"
curl -u admin:admin -X POST "localhost:8080/api/admin/ingestion/normalize"
```

Ingestion only stores raw payloads. Normalization is a separate pass so that a
mapping bug never costs a re-fetch from a rate-limited government API, and so
the mapping can be corrected and re-run against records already held.

Ingestion is triggered manually, never on a schedule and never from the public
read path, because it is a batch job that runs for minutes.

## Authentication

There is none on the read path, deliberately. Every funding record here is
public government data, so there is no privacy boundary and nothing for
per-user authorization to protect.

The only protected surface is `/api/admin/**` — ingestion and normalization —
behind HTTP Basic with a single operator credential from
`CIVICFUNDS_ADMIN_USERNAME` / `CIVICFUNDS_ADMIN_PASSWORD`.

## AI

`CIVICFUNDS_AI_ENABLED=false`, and **no AI runs in this build**. The
application is fully functional with no API key.

The design, for later: a model may only emit prose containing placeholder
tokens, and the backend substitutes every number itself. A generated paragraph
containing a bare digit is rejected. The model cannot state a wrong figure
because it is structurally prevented from stating any figure.

See `AI_USAGE.md` for how AI assistance was used in building this project.

## What is not built

Named here rather than left as a silence.

- **Cross-source project matching.** Deciding that three records describe the
  same road. Designed — deterministic identifier matching first, probabilistic
  scoring for the remainder, auto-merge only above a high threshold, a human
  review queue for the uncertain middle, decisions recorded against record
  pairs so they survive re-clustering. Not implemented. A matcher that merges
  two different bridges produces a wrong number, which is worse than no matcher.
- **The map.** Needs geocoding, council district boundaries in PostGIS, and
  point-in-polygon assignment.
- **State, city and school district adapters.** Three of six planned sources.
- **The `frontend/` directory** holds an early React scaffold that is not part
  of the running application.

## Licence and data

Code: see `LICENSE`. Government data indexed here is public record and remains
the property of the publishing agency; every record keeps a link to its source.

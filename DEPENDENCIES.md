# Dependencies and Data Sources — CivicFunds

Everything this project uses that it did not write, and where every record comes from.

---

## Open-source libraries

Versions marked *(managed)* are pinned by the Spring Boot 3.3.5 bill of materials.

| Library | Version | Licence | Why it is here |
|---|---|---|---|
| Spring Boot — Web | 3.3.5 *(managed)* | Apache-2.0 | HTTP layer and JSON |
| Spring Boot — Data JPA | *(managed)* | Apache-2.0 | Persistence; Hibernate underneath |
| Spring Boot — Validation | *(managed)* | Apache-2.0 | Jakarta Bean Validation on request parameters and DTOs |
| Spring Boot — Security | *(managed)* | Apache-2.0 | HTTP Basic on `/api/admin/**` only; the read path is public |
| Spring Boot — Actuator | *(managed)* | Apache-2.0 | Health endpoint for the host |
| Spring Boot — Test | *(managed)* | Apache-2.0 | JUnit 5, Mockito, MockMvc, AssertJ |
| Spring Security Test | *(managed)* | Apache-2.0 | Authenticated request helpers in tests |
| Flyway Core + PostgreSQL | *(managed)* | Apache-2.0 | Versioned schema migrations `V1`–`V6` |
| PostgreSQL JDBC driver | *(managed)* | BSD-2-Clause | Database connectivity |
| Apache Commons Text | 1.12.0 | Apache-2.0 | Jaro-Winkler string similarity for record matching |
| springdoc-openapi | 2.6.0 | Apache-2.0 | OpenAPI document and Swagger UI |
| Project Lombok | *(managed)* | MIT | `@Getter` / `@Setter` on JPA entities, to keep them readable |

## Database extensions

| Extension | Licence | Why it is here |
|---|---|---|
| PostGIS | GPL-2.0-or-later | Point-in-polygon assignment of records to council and congressional districts |
| `pg_trgm` | PostgreSQL Licence | Trigram similarity — fuzzy search and candidate blocking for matching |

## Fonts (prototype only)

| Font | Licence |
|---|---|
| Newsreader, Public Sans, IBM Plex Mono (Google Fonts) | SIL Open Font License 1.1 |

---

## Government data sources

No dataset is bundled in this repository. Every record is retrieved at run time from
a public government system and stored with its source URL and the date it was fetched.

| Source | Level | Access | Key / authentication | Status |
|---|---|---|---|---|
| **USAspending.gov** — federal awards with a place of performance in Duval, Clay or Nassau County | Federal | REST API, `POST /api/v2/search/spending_by_award/` | None required | **Ingesting** |
| **FDOT All Construction Contracts** — let contracts with amounts, coordinates and federal-aid numbers | State | ArcGIS REST | None required | Adapter designed, not built |
| **FDOT Work Program** — programmed items with corridor geometry | State | ArcGIS REST | None required | Adapter designed, not built |
| **Resilient Florida (FDEP)** — flood and resilience grant awards | State | ArcGIS REST + CSV export | None required | Adapter designed, not built |
| **Jacksonville City Council (Legistar)** — appropriation ordinances | Local | JSON API | None required | Adapter designed, not built |
| **North Florida TPO TIP** and **Jacksonville Capital Improvement Plan** — the federal / state / local split per transportation project, keyed by FM number | Local | Published PDF | n/a | Extracted semi-automatically, hand-checked |

**Terms of use.** All six are public records published by US federal, Florida state or
City of Jacksonville agencies. No source requires an API key, and none is accessed
behind authentication or in a way its terms prohibit.

**Rate limiting.** Ingestion is scheduled, not on the request path, and is
checksum-idempotent so a re-run does not re-fetch unchanged records.

**Honesty rule.** A record in the application is one of exactly two things, and the
interface says which: **Verified** — retrieved from a live source on a stated date — or
**Demo** — illustrative, used to design a screen, and never presented as government
funding.

# AI Usage Disclosure — CivicFunds

The Congressional App Challenge permits the use of AI tools provided their use is
disclosed. This file is that disclosure. It is kept current with every change to the
repository; an entry is added in the same commit as the work it describes.

**Summary.** CivicFunds was built by Ashwin, Aneesh and others working with an AI assistant (Claude,
via Claude Code / Cowork). The student defined the problem, the rules the architecture
must obey, and the scope; located and verified every government source document; and
performed all builds, test runs, database work, deployment and debugging — the AI has
never compiled or run this application. The AI wrote much of the implementation to the
student's specification, proposed designs, produced tests, and argued against several
of the student's own decisions. The division of labour is itemised below, entry by
entry.

---

## How we worked

- The student set the product direction, the scope decisions and the rules the
  application must obey (see `claude/PROJECT_STATE.md` §2, "the non-negotiables").
- The AI was used as an architect, pair programmer and reviewer: proposing designs,
  writing code to a stated spec, generating tests, and challenging decisions.
- Several significant decisions came from the AI *disagreeing* with the student's
  instruction and the student accepting the argument — these are logged in
  `claude/PROJECT_STATE.md` §3 as locked decisions with reasons.
- The AI also found factual errors in the student's own plan, including a wrong
  statement of the district's geography (see 9 Sep below).
- No code is in the repository that the student has not read.

---

## Log

| Date | Tool | What the AI did | What the student did | How it was validated | Student TODO |
|---|---|---|---|---|---|
| 2026-09-08 | Claude | Technical feasibility review of the student's specification — 20 sections, including an argument against several of the student's own choices (dropped JWT, dropped Neo4j, cut 4 data sources) | Wrote the original ~58-section specification; accepted or rejected each challenge | Checked each dropped source by hand against its published API | — |
| 2026-09-08 | Claude | Competitive benchmark against past CAC winners; rubric analysis | Supplied the district and prior-winner context | Every figure traced to a cited source URL in `CAC_BENCHMARK.md` | — |
| 2026-09-08 | Claude | Phase 1 — Flyway `V1`–`V5`, `Amount`/`AmountClass` value types, JPA entities, repositories, `SecurityConfig`, `MetaController` | Reviewed the schema; confirmed the append-only trigger behaviour | `mvn test` green locally; append-only trigger verified by hand with an `UPDATE` that correctly failed | **Re-read `Amount.plus()` and be able to explain why it throws** |
| 2026-09-08 | Claude | Phase 2 — USAspending adapter, client, normalizer, `IngestionOrchestrator`, admin ingestion endpoint, ~32 tests | Ran the real ingestion against `api.usaspending.gov` and inspected the stored rows | Live API call returned real Duval/Clay/Nassau records; checksum idempotency verified by re-running | **Verify spike S10: does `Award Amount` equal `total_obligation` for every award type?** |
| 2026-09-09 | Claude | Interactive HTML prototype (8 screens) used as the design reference | Directed the design; rejected and redirected several versions | Rendered and reviewed screen by screen; three layout bugs and one contrast bug found and fixed from screenshots | — |
| 2026-09-09 | Claude | Adversarial "AI-necessity review" — argued *against* the project's own AI claims and concluded AI is not necessary to ~85% of it | Asked for the review and accepted its conclusions | Recorded in `claude/AI_NECESSITY_REVIEW.md` | **Be able to state what AI does and does not do in this app** |
| 2026-09-09 | Claude | Caught a factual error in the student's scope: FL-04 is **not** three whole counties — it splits Duval at the St. Johns River | Accepted the correction and re-scoped to county-level indexing | Verified against Rep. Bean's office page and two independent sources | — |
| 2026-10-20 | Claude | Phase 3 — search: Flyway `V6` (weighted `tsvector`, `pg_trgm` indexes), `IdentifierQuery`, `MatchKind`, `FundingSearchService`, `FundingSearchController`, DTOs, 3 test classes | Specified the three-rung behaviour and the rule that every figure travels with its measurement class | *Written but not compiled by the AI* — Maven Central is unreachable from its sandbox, so this code was unverified until 25 Oct | Done 25 Oct — see below |
| 2026-10-20 | Claude | Phase 4 — the shipped front end: `index.html`, `app.css`, `app.js`. Plain HTML/CSS/JS, no framework | Chose to drop the React front end for the submission build | Rendered and reviewed from screenshots | — |
| 2026-10-20 | Claude | `DEPENDENCIES.md`, and this disclosure file | Asked for both | — | — |
| 2026-10-25 | Student | **Compiled and ran the whole project for the first time**: installed nothing (no Docker, Windows Package Manager disabled by policy), provisioned PostgreSQL 16 on Neon, ran `mvn test` | All of it | **53 tests, 0 failures.** Phase 3 verified at last | — |
| 2026-10-25 | Student | Diagnosed the ingestion failure: `PKIX path building failed`, caused by corporate TLS interception. Fixed by pointing the JVM at the Windows trust store | All of it — the AI suggested the trust store flag after the student produced the stack trace and the `curl --ssl-no-revoke` evidence | Ingestion then returned 300 live records | — |
| 2026-10-25 | Claude | Found that `UsaSpendingNormalizer` was never called by anything — ingestion stored raw rows and nothing became searchable. Wrote `NormalizationService` and the `/api/admin/ingestion/normalize` endpoint | Ran it; confirmed 850 of 850 normalized, 0 failed | Record count verified against `/api/meta/coverage` | **Be able to say why normalization is a separate pass from ingestion** |
| 2026-10-25 | Claude | Fixed `AwardTypeGroup`: the API rejected a group that mixed grants, other financial assistance and direct payments. Corrected the groupings from the API's own 422 response and added the missing `F00x` codes | Ran the ingestion that produced the 422 and supplied the error body | All six groups then returned records; 850 stored | — |
| 2026-10-25 | Claude | Changed the record cap from a global truncation to a per-group quota, after the first 300 records turned out to be contracts only — which have no CFDA number and so left identifier matching untestable | Approved the change | `recordsWithDeterministicKey` went from 0 to 553 | — |
| 2026-10-25 | Student + Claude | Three records transcribed by hand from published PDFs (North Florida TPO TIP, FDOT Annual Obligations Report, FHWA Eastern Federal Lands TIP) | **Found and read every figure in the source PDFs**; rejected an AI-generated summary of a different project because its citations did not support its number | Each figure checked against the printed page; `source_system` ends in `MANUAL` and shows on screen | — |
| 2026-10-25 | Claude | Fixed a bug the student's own test caught: `weakTitle()` let the procurement code `IGF::OT::IGF` become a project title, because the guard's character class omitted the colon | Ran the test suite that failed | Test `doesNotInventTitlesFromCodes` then passed; 53/53 green | — |
| 2026-10-25 | Claude | Overview and About pages: `/api/meta/overview`, five count queries, `overview.html/.js`, `about.html`, nav | Asked for them; decided which panels were honest to show | Rendered and checked against the database counts | — |
| 2026-10-25 | Claude | Rewrote this README to match what was actually built, replacing a September version that described six data sources and a React front end that do not exist in the shipped app | Asked for it | Every claim checked against the running app | — |

---

## What the AI did not do

- **It never compiled or ran this project.** Maven Central and `api.usaspending.gov`
  are both unreachable from the environment the AI runs in. Every build, every test
  run, every ingestion and every database migration was executed by the student on
  their own machine. The AI has never seen this application work.
- It did not solve the environment problems, which were most of the work on 25
  October: no Docker, the Windows Package Manager disabled by Group Policy, corporate
  TLS interception breaking the JVM's trust store, and provisioning a cloud database.
  The AI suggested; the student diagnosed and executed.
- It did not find or read the source PDFs. The student located every hand-entered
  figure in the published documents.
- It did not make the product decisions. The refusal to display a combined total, the
  scope, the choice of sources, and the decision to cut the matching engine from this
  submission are the student's.
- It never wrote a figure that appears in the application. Every number in the running
  app comes from an ingested or transcribed government record with a source URL.

## What the student should be able to explain

Listed here because the Challenge expects "significant individual contributions and
technical understanding", and because it is a fair test of whether this collaboration
produced understanding or only code.

1. Why `Amount.plus()` throws, and what would go wrong if it returned a value.
2. What the append-only trigger on `raw_funding_record` prevents, and why that matters
   when a government revises a published figure.
3. Why search is three separate queries rather than one combined one.
4. Why ingestion and normalization are separate passes.
5. Why an obligation and a programmed amount cannot be added — and why two figures of
   the *same* class still cannot always be added.

## Known limitation in the prototype

The interactive HTML prototype in this repository contains **illustrative data** used
to design the interface. It is labelled Demo on screen. It is not the application and
is not presented as working software.

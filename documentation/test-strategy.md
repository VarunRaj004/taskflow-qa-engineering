# TaskFlow QA — Test Strategy

## Test types and how each is evidenced

| Type | Approach | Evidence location |
|---|---|---|
| Functional | Each requirement in `requirements-specification.md` is exercised via its mapped test case(s). | `test-cases.csv`, `test-execution-report.md` |
| Negative | Invalid, missing, malformed and unauthorized inputs for every endpoint/form. | Same, plus `api-testing/` collection's negative requests |
| Boundary-value | Title at 0/1/200/201 chars, password at 7/8 chars, empty vs long description, etc. | `test-cases.csv` (BV-tagged cases) |
| UI / usability | Visible labels, inline validation messages, empty states, keyboard-operable forms. | `test-cases.csv`, screenshots in `bug-reports/evidence/` where a defect was found |
| API | Status code, content-type, response schema, auth/ownership enforcement, error shape, response time. | `api-testing/postman-collection.json`, Newman HTML/CLI output in `reports/` |
| Database | Persistence of create/update/delete, ownership/foreign-key integrity, no orphaned rows. | `database-testing/validation-queries.sql` + captured (redacted) output |
| Regression | Any case that failed is re-run after a fix, plus the directly related cases for that area. | `regression-test-report.md` |
| Exploratory | Short, undirected sessions clicking through realistic and unusual sequences (double-submit, rapid filter changes, browser back after logout). | Session notes in `test-execution-report.md`'s exploratory section |

## Prioritization

Authentication and ownership/access-control are treated as highest priority (a failure there is
a security-relevant defect), followed by task CRUD correctness, then dashboard consistency, then
UI/usability polish.

## Pass / Fail / Blocked / Not Run discipline

- **Pass** — executed, actual result matches expected result exactly.
- **Fail** — executed, actual result differs from expected; linked to a defect ID in
  `defect-report.md` once reproduced a second time.
- **Blocked** — could not be executed (e.g. depends on an unimplemented feature, or a
  prerequisite step failed); the blocking reason is recorded, not guessed around.
- **Not Run** — in scope but not executed in this pass (time-boxing); never counted as a Pass.

A test is never marked Pass from reading the source code alone — it must be actually run
(manually, via Postman/Newman, via Playwright, or via a direct SQL query against the real test
database) and the real output observed.

## Defect confirmation

A defect is logged in `defect-report.md` only once the same unexpected behaviour has been
reproduced on a second, independent execution (not just "it happened once"). A single anomalous
observation that doesn't reproduce is noted as an exploratory finding, not a confirmed defect.

## Regression approach

After any backend/frontend fix during this project, the specific failed case plus every other
case touching the same endpoint/component is re-executed before the case is marked Pass, and the
before/after pair is recorded in `regression-test-report.md`.

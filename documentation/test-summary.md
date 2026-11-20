# TaskFlow QA — Final Test Summary

Real, executed metrics only — see `test-execution-report.md` for the raw tool output behind
every number here, and `test-cases.csv` for the full per-case register.

## Headline numbers

- **78 manual/API/UI test cases designed**, mapped to 30 traceable requirement IDs in
  `requirements-specification.md`.
- **69 Pass, 3 Fail, 6 Not Run, 0 Blocked.**
- **41 Postman requests / 68 assertions executed via Newman** — 66 passed, 2 failed (both
  confirmed defects).
- **11 Playwright end-to-end tests** — all 11 passing in the final run (10 initially failed due
  to a now-fixed defect; see below).
- **4 confirmed defects** (two discovered by design — BUG-001/BUG-002 — and two discovered
  through genuine testing — BUG-003/BUG-004, not anticipated when the test suite was written).
  One fixed and regression-verified (BUG-003); three left open and documented.

## What was actually verified

- Every endpoint in `API_CONTRACT.md` — happy path, negative inputs, and boundary values
  (password/name/title/description length limits) — executed against a real running backend,
  not inferred from reading the source.
- Ownership/access-control isolation between two real synthetic users, at both the API layer
  (Newman) and through two independent real browser sessions (Playwright).
- Database persistence (create/update/delete, password hashing, dashboard-count consistency)
  verified with direct SQL against the same MySQL instance the API was using — not assumed from
  the API's responses alone.
- Critical UI journeys (register, login, invalid login, logout, protected-route redirect, task
  CRUD, status change, empty-state search) executed end-to-end in a real Chromium browser.

## Confirmed defects

| ID | Severity | Status | Summary |
|---|---|---|---|
| BUG-001 | Low | Open | `PUT /api/tasks/{id}` accepts a whitespace-only title (create correctly rejects it; update doesn't) |
| BUG-002 | Low | Open | Task search is case-sensitive, contradicting the documented contract |
| BUG-003 | Critical | **Fixed, regression-verified** | CORS was disabled, blocking every frontend→backend request in a real browser |
| BUG-004 | Medium | Open | Sorting tasks by priority orders alphabetically, not by severity (HIGH/MEDIUM/LOW) |

Full reproduction steps, evidence, and the fix/regression detail for BUG-003 are in
`defect-report.md` and `regression-test-report.md`.

## Known limitations

- **Six test cases were not executed in this pass** (time-boxed to a single build session
  rather than the specification's illustrative 14-day plan): keyboard-only form submission,
  long-title layout/overflow, non-Chromium rendering (also explicitly out of scope), two
  exploratory double-submit/back-button checks, and recovering from a mid-session backend
  outage. These are recorded as `Not Run` with a reason in `test-cases.csv`, not silently
  skipped or marked Pass.
- **BUG-001, BUG-002 and BUG-004 were deliberately left unfixed.** BUG-001/BUG-002 are low
  severity and were part of the original scope for this exercise to find and document, not
  silently patch. BUG-004 was a genuine discovery during this pass; fixing it correctly needs a
  deliberate severity-rank mapping (not a one-line change) and was judged out of scope for this
  pass rather than rushed.
- **No formal load/performance testing.** API response times were recorded as informational
  data from the Newman run (avg 109ms, max 902ms across 41 requests on a local machine) — this
  is not a load test and makes no throughput/concurrency claim.
- **No full penetration test.** Security-oriented checks covered authentication enforcement,
  authorization boundaries (ownership isolation, no password leakage), and basic input
  validation — not a comprehensive security assessment.
- **Single-browser coverage (Chromium only),** per the specification's own minimum and the
  project's agreed scope.
- **The AUT itself is a purpose-built demo**, not a pre-existing production application — see
  the top-level README and `requirements-specification.md` for why, and for what's explicitly
  out of scope in the demo app (pagination, roles/multi-tenancy, password reset, refresh
  tokens).
- **One design observation, not a defect:** `tasks.owner_id` has no database-level foreign-key
  constraint to `users.id` (confirmed via `database-testing/validation-queries.sql` DV-01b).
  Referential integrity is enforced entirely at the application layer; see `defect-report.md`
  for why this wasn't logged as a defect.

## Independent portfolio statement

This project, including the demo application it tests, was built and executed as an
independent QA engineering portfolio exercise. It is not a claim of employment experience, and
the demo AUT is not a real product.

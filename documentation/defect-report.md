# TaskFlow QA — Defect Report

Every defect below was reproduced on two independent executions before being logged here, per
`test-strategy.md`'s defect-confirmation rule. Nothing here is a hypothetical — see each
entry's evidence file for the exact requests/responses captured.

---

### BUG-003 — CORS disabled: the frontend cannot call the backend at all

- **Module:** Cross-cutting (Authentication / Task management / Dashboard — everything)
- **Environment / build:** `aut-demo/backend` (pre-fix), `aut-demo/frontend` served from a
  different origin/port (Vite dev server, `http://localhost:5173`) than the API
  (`http://localhost:8090`)
- **Severity:** Critical (blocks 100% of real browser usage of the application — any deployment
  with the frontend and backend on different origins, which is the normal case, is completely
  unusable)
- **Priority:** P1
- **Linked test case:** found via `automation/playwright/` (all UI journeys), not a single
  manual test case — see `test-execution-report.md`
- **Preconditions:** frontend and backend both running, on different origins (the default dev
  setup documented in the top-level README)

**Steps to reproduce**
1. Start the backend (`aut-demo/backend`, port 8090) and frontend (`aut-demo/frontend`, Vite
   dev server, port 5173).
2. Open `http://localhost:5173/register` in a real browser.
3. Fill in the registration form with any valid synthetic details and submit.

**Expected result:** the registration request succeeds (or fails with a real validation error
from the API) and the user is taken to `/login`.

**Actual result:** the browser's CORS preflight (`OPTIONS /api/auth/register`) returns `403`,
the browser blocks the actual `POST` with `net::ERR_FAILED`, and the frontend shows nothing
more specific than "Something went wrong" — because `SecurityConfig.java` explicitly disabled
CORS (`.cors(cors -> cors.disable())`), so no `Access-Control-Allow-Origin` header was ever
sent. This blocked every authenticated journey: 9 of 11 Playwright tests timed out waiting for
navigations/responses that never arrived.

**Evidence:** [`bug-reports/evidence/BUG-003-cors-preflight.txt`](../bug-reports/evidence/BUG-003-cors-preflight.txt)
(console + network log), first Playwright run (9/11 failed, all with the same root cause).

**Status:** Fixed

**Verification after fix:** added a `CorsConfigurationSource` bean permitting the frontend's
origin (`aut-demo/backend/.../config/SecurityConfig.java`, configurable via
`CORS_ALLOWED_ORIGINS`). Backend's own `mvn test` suite re-run clean (25/25) after the change.
The identical manual repro steps above now succeed with no console errors, and the full
Playwright suite was re-run: **11/11 passed**. See `regression-test-report.md`.

---

### BUG-001 — `PUT /api/tasks/{id}` accepts a whitespace-only title

- **Module:** Task management
- **Environment / build:** `aut-demo/backend`, local test database
- **Severity:** Low (cosmetic/data-quality — a task can end up with an unreadable blank title;
  no security or data-loss impact)
- **Priority:** P3
- **Linked test case:** TC-TASK-012 (see `test-cases.csv`); also covered in
  `api-testing/postman-collection.json` ("Update task - whitespace-only title per REQ-TASK-002")
- **Preconditions:** one authenticated synthetic user, one existing task they own

**Steps to reproduce**
1. Log in and note an existing task's id.
2. `PUT /api/tasks/{id}` with body `{"title": "   "}` (three spaces).

**Expected result (REQ-TASK-002):** `400 Bad Request`, same as `POST /api/tasks` with the same
title value (which correctly rejects it).

**Actual result:** `200 OK`; the task is persisted with the literal whitespace title, confirmed
directly in the database (`SELECT title FROM tasks WHERE id = ...` returns `"   "`).

**Evidence:** [`bug-reports/evidence/BUG-001-blank-title-update.txt`](../bug-reports/evidence/BUG-001-blank-title-update.txt)

**Status:** Open (not fixed — see `test-summary.md` for why the two non-blocking defects were
left open rather than silently patched)

**Verification after fix:** n/a — not yet fixed.

---

### BUG-002 — `GET /api/tasks?search=` is case-sensitive, contradicting the documented contract

- **Module:** Task management
- **Environment / build:** `aut-demo/backend`, local test database
- **Severity:** Low (a usability/search-quality defect, not a data-integrity or security issue)
- **Priority:** P3
- **Linked test case:** TC-TASK-009 (see `test-cases.csv`); also covered in
  `api-testing/postman-collection.json` ("List tasks - search is case-insensitive per
  REQ-TASK-007")
- **Preconditions:** one authenticated synthetic user with a task whose title is known (e.g.
  "Buy groceries")

**Steps to reproduce**
1. Create a task titled e.g. "Buy groceries".
2. `GET /api/tasks?search=GROCERIES` (different case than the stored title).

**Expected result (REQ-TASK-007 / API_CONTRACT.md):** the task is returned — search should
match case-insensitively.

**Actual result:** `{"tasks":[],"count":0}` — zero results. An exact-case search does match,
confirming the defect is specifically the missing case-insensitivity.

**Evidence:** [`bug-reports/evidence/BUG-002-case-sensitive-search.txt`](../bug-reports/evidence/BUG-002-case-sensitive-search.txt)

**Status:** Open (not fixed — see `test-summary.md`)

**Verification after fix:** n/a — not yet fixed.

---

### BUG-004 — `sortBy=priority` sorts alphabetically, not by priority severity

- **Module:** Task management
- **Environment / build:** `aut-demo/backend`, local test database
- **Severity:** Medium (a correctness/usability defect in a feature users would actively rely
  on — "sort by priority" silently does not mean what it says — though it has no security or
  data-integrity impact)
- **Priority:** P2
- **Linked test case:** TC-TASK-029 (see `test-cases.csv`)
- **Preconditions:** a user with tasks spanning all three priority values

**Steps to reproduce**
1. As a user, create one task each with priority `LOW`, `HIGH`, and `MEDIUM` (in that order, so
   creation order cannot explain the result).
2. `GET /api/tasks?sortBy=priority&sortDir=desc`.

**Expected result:** tasks ordered most-urgent-first: `HIGH`, `MEDIUM`, `LOW`.

**Actual result:** tasks ordered `MEDIUM`, `LOW`, `HIGH` — alphabetical-looking order, not
severity order. The ascending case is the mirror image (`HIGH`, `LOW`, `MEDIUM`), confirming
it's sorting lexicographically rather than by the severity rank the `TaskPriority` enum
declares (`LOW, MEDIUM, HIGH`).

**Evidence:** [`bug-reports/evidence/BUG-004-priority-sort-order.txt`](../bug-reports/evidence/BUG-004-priority-sort-order.txt)
— reproduced on two independent executions with different users/task sets.

**Status:** Open (not fixed — discovered during this testing pass; fixing it correctly needs an
explicit severity-rank mapping rather than a plain column sort, which was judged out of scope
for this pass; see `test-summary.md`)

**Verification after fix:** n/a — not yet fixed.

---

## Observations recorded but not logged as defects

- **No database-level foreign key from `tasks.owner_id` to `users.id`.** Confirmed via
  `database-testing/validation-queries.sql` (DV-01b returns zero rows). Referential integrity
  is enforced entirely at the application layer (every task-returning endpoint already filters
  by the authenticated user's id, and DV-06 confirms there are no orphaned rows in practice).
  Not logged as a defect because no requirement specifies a DB-level constraint and no
  observed behaviour violates a requirement — recorded here as a design note for anyone
  extending the schema.

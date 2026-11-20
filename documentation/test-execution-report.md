# TaskFlow QA — Test Execution Report

**Build tested:** `aut-demo/backend` (local build, post CORS-fix commit) + `aut-demo/frontend`
(local build) + MySQL 8 (Docker, local). **Date:** 2026-11-20. Full per-case detail is in
[`test-cases.csv`](test-cases.csv) (78 cases: 69 Pass, 3 Fail, 6 Not Run, 0 Blocked). This
document gives the raw tool output behind that register.

## 1. API tests (Postman / Newman)

Command: `npx newman run api-testing/postman-collection.json -e api-testing/postman-environment.example.json --env-var baseUrl=http://localhost:8090`

```
┌─────────────────────────┬────────────────────┬───────────────────┐
│                         │           executed │            failed │
├─────────────────────────┼────────────────────┼───────────────────┤
│              iterations │                  1 │                 0 │
├─────────────────────────┼────────────────────┼───────────────────┤
│                requests │                 41 │                 0 │
├─────────────────────────┼────────────────────┼───────────────────┤
│            test-scripts │                 41 │                 0 │
├─────────────────────────┼────────────────────┼───────────────────┤
│      prerequest-scripts │                  4 │                 0 │
├─────────────────────────┼────────────────────┼───────────────────┤
│              assertions │                 68 │                 2 │
├─────────────────────────┴────────────────────┴───────────────────┤
│ total run duration: 8.3s                                         │
├──────────────────────────────────────────────────────────────────┤
│ total data received: 5.23kB (approx)                             │
├──────────────────────────────────────────────────────────────────┤
│ average response time: 109ms [min: 9ms, max: 902ms, s.d.: 161ms] │
└──────────────────────────────────────────────────────────────────┘
```

**2 failing assertions, both confirmed real defects (not flaky/environmental):**
1. `List tasks - search is case-insensitive per REQ-TASK-007` → **BUG-002**
2. `Update task - whitespace-only title per REQ-TASK-002` → **BUG-001**

All 66 other assertions passed, covering every endpoint in `API_CONTRACT.md`.

## 2. Additional manual API checks (not in the Postman collection)

Run directly via `curl` against the same running backend, to cover boundary values and a few
cases the collection doesn't already exercise:

| Check | Request | Result |
|---|---|---|
| Missing email on register | `POST /api/auth/register` (no `email`) | `400`, `{"error":"Email is required"}` |
| Password exactly 8 chars (boundary) | register with `password="Exactly8"` | `201` |
| Password 7 chars (boundary) | register with `password="Seven12"` | `400`, `{"error":"Password must be at least 8 characters"}` |
| Name exactly 100 chars (boundary) | register with a 100-char name | `201` |
| Name 101 chars (boundary) | register with a 101-char name | `400`, `{"error":"Name must be at most 100 characters"}` |
| Title exactly 200 chars (boundary) | `POST /api/tasks` | `201` |
| Description exactly 2000 chars (boundary) | `POST /api/tasks` | `201` |
| Description 2001 chars (boundary) | `POST /api/tasks` | `400`, `{"error":"Description must be at most 2000 characters"}` |
| Expired JWT (hand-crafted, same HS256 secret, `exp` in the past) | `GET /api/users/me` | `401`, `{"error":"Unauthorized"}` |
| Fresh user, dashboard | `GET /api/dashboard` | `200`, `{"total":0,"todo":0,"inProgress":0,"done":0}` |
| Fresh user, task list | `GET /api/tasks` | `200`, `{"tasks":[],"count":0}` |
| Invalid status filter value | `GET /api/tasks?status=NOT_A_STATUS` | `400`, `{"error":"Invalid status value"}` |
| Combined status+priority filter | `GET /api/tasks?status=TODO&priority=HIGH` | `200`, only matching tasks returned |
| Partial update (description only) | `PUT /api/tasks/{id}` with only `description` | `200`, only that field changed |
| List tasks, no auth | `GET /api/tasks` (no header) | `401` |
| **Sort by priority, descending/ascending** | `GET /api/tasks?sortBy=priority&sortDir=desc\|asc` | **Not severity-ordered — see BUG-004** |

All of the above passed except the priority-sort check, which surfaced **BUG-004** (new finding
from this pass, confirmed on two independent executions with different users — see
`defect-report.md` and `bug-reports/evidence/BUG-004-priority-sort-order.txt`).

## 3. Database validation

Run via `docker exec taskflow-mysql mysql -u taskflow -p... taskflow` against the local
MySQL container, using the queries in `database-testing/validation-queries.sql`:

- **DV-01 (schema sanity):** `users` and `tasks` tables and columns match the entity mapping
  exactly (types, nullability, keys).
- **DV-01b (FK check):** zero rows — confirmed there is no database-level foreign key from
  `tasks.owner_id` to `users.id` (recorded as an observation, not a defect — see
  `defect-report.md`).
- **DV-02 (password hashing):** the registered test user's `password_hash` column starts with
  `$2a$10$` (bcrypt), never the plaintext password.
- **DV-03 (task persistence):** a task updated via the API to a whitespace title (BUG-001) was
  confirmed persisted exactly as `"   "` in the `title` column — the bug is a real, persisted
  data-quality issue, not just an API response artifact.
- **DV-05 (delete persistence):** after `DELETE /api/tasks/{id}` returned `204`, `SELECT COUNT(*) FROM tasks WHERE id = ...` returned `0` — confirmed hard delete, not soft delete.
- **DV-06 (orphan check):** zero rows — no task references a non-existent user.
- **DV-07 (dashboard consistency):** `SELECT COUNT(*), SUM(status='TODO'), ...` for the test
  user matched the live `GET /api/dashboard` response exactly (`total=1, todo=1, inProgress=0,
  done=0` at time of check).

## 4. UI automation (Playwright)

Command: `npx playwright test` (from `automation/playwright/`), Chromium.

**First run (before the CORS fix) — 1 passed, 10 failed:**

```
ok  protected pages redirect an unauthenticated visitor to /login            (5.3s)
x   a user cannot view or modify another user's task                        (31.8s, timeout)
x   a user can register and then log in with valid credentials              (33.1s, timeout)
x   invalid login is rejected with an understandable message                (failed assertion)
x   log out ends the session and returns to a logged-out state              (33.5s, timeout)
x   an authenticated user can create a task                                 (32.8s, timeout)
x   creating a task with a blank title is rejected                          (33.3s, timeout)
x   ...update a task, and it survives a refresh                            (34.1s, timeout)
x   task status can be changed                                              (38.0s, timeout)
x   an authenticated user can delete a task                                 (52.6s, timeout)
x   an empty / no-match task list shows a clear empty state                 (32.4s, timeout)
```

Root cause: BUG-003 (CORS disabled). See `defect-report.md` and
`bug-reports/evidence/BUG-003-cors-preflight.txt`.

**Final run (after the CORS fix, and after fixing one unrelated Playwright locator bug) — 11/11
passed.** Full before/after detail, including the backend regression suite re-run, is in
[`regression-test-report.md`](regression-test-report.md).

HTML report: `reports/playwright-html-report/index.html` (open directly, or
`npm run report` from `automation/playwright/`). Raw traces/screenshots/videos for the one
pre-fix failure set are under `reports/playwright-results/` from that run.

## 5. Exploratory testing notes

- **Repeated failed login attempts (5x):** each attempt independently returned `401` with no
  lockout, rate-limiting, or escalating delay — consistent with "no lockout implemented" being
  out of scope (`requirements-specification.md`), not a defect.
- **Rapid double-click on "Save Task"** and **browser back-button after logout** were
  identified as worthwhile exploratory checks but not executed in this time-boxed pass — see
  TC-UI-009/TC-UI-010 in `test-cases.csv` (Not Run) and `test-summary.md`.

## 6. Summary of confirmed defects this pass

| ID | Title | Severity | Status |
|---|---|---|---|
| BUG-001 | Blank/whitespace title accepted on task update | Low | Open |
| BUG-002 | Search is case-sensitive, contradicting the contract | Low | Open |
| BUG-003 | CORS disabled — blocked all frontend/backend communication | Critical | **Fixed** |
| BUG-004 | Sort by priority is alphabetical, not severity-ranked | Medium | Open |

Full detail for each: [`defect-report.md`](defect-report.md).

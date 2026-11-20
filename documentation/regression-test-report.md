# TaskFlow QA — Regression Test Report

Only one defect in this project (BUG-003) was fixed during the testing pass, so this report
covers that single before/after regression cycle.

## BUG-003 — CORS disabled

### Before (initial Playwright run, `automation/playwright/`)

```
Running 11 tests using 1 worker
  x  access-control.spec.js › a user cannot view or modify another user's task        (31.8s, timeout)
  x  auth.spec.js › register and log in with valid credentials                        (33.1s, timeout)
  x  auth.spec.js › invalid login is rejected with an understandable message          (failed assertion)
  ok auth.spec.js › protected pages redirect an unauthenticated visitor to /login      (5.3s)
  x  auth.spec.js › log out ends the session and returns to a logged-out state        (33.5s, timeout)
  x  tasks.spec.js › an authenticated user can create a task                          (32.8s, timeout)
  x  tasks.spec.js › creating a task with a blank title is rejected                    (33.3s, timeout)
  x  tasks.spec.js › an authenticated user can update a task...                        (34.1s, timeout)
  x  tasks.spec.js › task status can be changed                                        (38.0s, timeout)
  x  tasks.spec.js › an authenticated user can delete a task                           (52.6s, timeout)
  x  tasks.spec.js › an empty / no-match task list shows a clear empty state           (32.4s, timeout)

1 passed, 10 failed
```

Root cause isolated via the browser's own console/network log (see
`bug-reports/evidence/BUG-003-cors-preflight.txt`): every cross-origin request from the
frontend to the backend was blocked by the browser because CORS was disabled server-side.

### Fix applied

`aut-demo/backend/src/main/java/com/taskflow/demo/config/SecurityConfig.java` — replaced
`.cors(cors -> cors.disable())` with a `CorsConfigurationSource` bean permitting the frontend's
origin(s) (configurable via `CORS_ALLOWED_ORIGINS`, defaulting to the local dev server).

### Regression executed after the fix

1. **Backend unit/integration tests** (`mvn test`, H2/test profile) — re-run in full:
   **25/25 passed**, no regressions from the security-config change.
2. **Manual repro of the original failure** — the exact steps that originally failed (register
   through the real UI at `http://localhost:5173/register` against the backend at
   `http://localhost:8090`) were repeated: no console errors, registration succeeds, redirect
   to `/login` happens as expected.
3. **Full Playwright suite** (the actual regression target, since it covers every journey the
   bug blocked) — re-run in full:

```
Running 11 tests using 1 worker
  ok access-control.spec.js › a user cannot view or modify another user's task          (10.0s)
  ok auth.spec.js › register and log in with valid credentials                          (2.8s)
  ok auth.spec.js › invalid login is rejected with an understandable message             (2.1s)
  ok auth.spec.js › protected pages redirect an unauthenticated visitor to /login         (2.1s)
  ok auth.spec.js › log out ends the session and returns to a logged-out state           (3.7s)
  ok tasks.spec.js › an authenticated user can create a task                             (3.7s)
  ok tasks.spec.js › creating a task with a blank title is rejected                      (3.9s)
  ok tasks.spec.js › an authenticated user can update a task...                          (8.4s)
  ok tasks.spec.js › task status can be changed                                         (27.5s)
  ok tasks.spec.js › an authenticated user can delete a task                             (4.5s)
  ok tasks.spec.js › an empty / no-match task list shows a clear empty state             (3.6s)

11 passed
```

(One unrelated test-authoring bug was found and fixed along the way: the first re-run still
failed 1/11 on `task status can be changed` with `locator.selectOption: strict mode violation:
getByLabel('Status') resolved to 2 elements` — the Tasks page has both a status *filter* select
and the task *form*'s status select, both labelled "Status". This was a bug in the Playwright
test's locator, not in the application; scoping the locator to `form.task-form` fixed it. This
is noted here for transparency, not logged in `defect-report.md` since it was never a defect in
the AUT.)

### Result

BUG-003 is confirmed fixed with no regressions in either the backend's own test suite or the
UI journeys it had blocked. BUG-001 and BUG-002 remain open (see `defect-report.md`) and were
intentionally not touched by this fix.

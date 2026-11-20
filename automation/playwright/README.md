# Playwright end-to-end tests

Covers the critical user journeys against the running TaskFlow demo AUT: register/login,
invalid login, protected-route redirects, logout, task create/update/delete, status change,
empty-state search, and cross-user access-control isolation (REQ-ACCESS-001/002/003).

## Prerequisites

1. MySQL running: `docker compose up -d` from `aut-demo/` (see that folder's README).
2. Backend running: from `aut-demo/backend/`, `mvn spring-boot:run` (reads `.env` — copy from
   `.env.example`). Note the port it starts on.
3. Frontend running: from `aut-demo/frontend/`, `npm run dev` (reads `.env` — copy from
   `.env.example`, pointing `VITE_API_BASE_URL` at the backend's port).
4. Copy `.env.example` to `.env` in this directory and set `BASE_URL` (the frontend's URL) and
   `API_BASE_URL` (the backend's URL) to match.

## Install & run

```powershell
npm install
npx playwright install chromium
npm test
```

Headed (watch the browser): `npm run test:headed`.

## Reports

An HTML report is written to `reports/playwright-html-report/` (relative to the repo root) and
raw artifacts (screenshots/traces/videos on failure) to `reports/playwright-results/`. Open the
report with:

```powershell
npm run report
```

or just open `reports/playwright-html-report/index.html` in a browser. Traces can be viewed with
`npx playwright show-trace <trace.zip>`.

## Notes

- Every test registers its own fresh synthetic user (unique, timestamped email) — no shared or
  hardcoded test accounts, so tests don't interfere with each other or require manual reset.
- `access-control.spec.js` drives two independent browser contexts (two real "logged in"
  sessions) and, for the part of the journey the UI has no route for (there is no per-task URL
  to navigate to directly), makes the cross-user API calls using each context's own token read
  from `localStorage` — this is still testing the same access-control boundary a malicious user
  would probe, just without re-implementing a raw HTTP client.
- `retries` is only set for CI (`process.env.CI`), per the specification's instruction not to
  hide flakiness locally.

# TaskFlow QA Engineering

An independent QA engineering portfolio project: a structured test strategy, manual test
design and execution, REST API testing, database validation, Playwright end-to-end automation,
defect documentation and a GitHub Actions CI pipeline, built and executed against a real running
application rather than written speculatively.

**This is an independent portfolio project**, not employment experience, and not a claim about
any employer's or client's product.

## What is being tested

No pre-existing React + Spring Boot + MySQL task-management application was available to test
against, so a small, purpose-built demo application (`aut-demo/`) was created specifically to
give this QA process something real to exercise end-to-end. The demo app is clearly out of
scope as a "product" — it exists only to be tested. See
[`aut-demo/API_CONTRACT.md`](aut-demo/API_CONTRACT.md) for exactly what it does.

## Architecture / test layers

| Layer | Tool | Location |
|---|---|---|
| Requirements & test design | Markdown / CSV | `documentation/` |
| API testing | Postman + Newman | `api-testing/` |
| Database validation | Parameterized SQL against MySQL | `database-testing/` |
| UI end-to-end automation | Playwright (JavaScript) | `automation/playwright/` |
| Defect tracking | Markdown defect register | `bug-reports/`, `documentation/defect-report.md` |
| CI | GitHub Actions | `.github/workflows/qa-tests.yml` |
| Application under test | React + Spring Boot + MySQL | `aut-demo/` |

## Repository map

```
taskflow-qa-engineering/
├── README.md                      <- you are here
├── aut-demo/                      <- synthetic demo Application Under Test
│   ├── API_CONTRACT.md            <- source of truth for endpoints/UI contract
│   ├── backend/                   <- Spring Boot 3 / Java 21
│   ├── frontend/                  <- React (Vite)
│   └── docker-compose.yml         <- local MySQL for dev/testing
├── documentation/                 <- requirements, test plan/strategy, test cases, reports
├── api-testing/                   <- Postman collection + example environment
├── automation/playwright/         <- Playwright E2E suite
├── database-testing/              <- SQL validation queries
├── bug-reports/                   <- defect template + evidence
├── reports/                       <- where generated run output lands
└── .github/workflows/qa-tests.yml <- CI pipeline
```

## Prerequisites

- Java 21, Maven, Node.js 20+, Docker (for local MySQL), `gh`/git (for cloning)
- `npm install -g newman newman-reporter-htmlextra` (or use the per-project `npx newman`)

## Environment setup

```powershell
# 1. MySQL (from aut-demo/)
cd aut-demo
Copy-Item .env.example .env   # edit if port 3306 is already taken locally
docker compose up -d

# 2. Backend (from aut-demo/backend/)
cd backend
Copy-Item .env.example .env   # set DB_PORT / SERVER_PORT to match step 1 if you changed them
mvn spring-boot:run

# 3. Frontend (from aut-demo/frontend/)
cd ../frontend
Copy-Item .env.example .env   # point VITE_API_BASE_URL at the backend's actual port
npm install
npm run dev
```

## Running the tests

**Manual** — see `documentation/test-cases.csv` and `documentation/test-execution-report.md`
for the register, the execution discipline, and the recorded results.

**API (Postman/Newman)** — from the repository root, with the backend running:

```powershell
npx newman run api-testing/postman-collection.json `
  -e api-testing/postman-environment.example.json `
  --env-var baseUrl=http://localhost:8090
```

**Database** — see `database-testing/README.md`.

**UI (Playwright)** — from `automation/playwright/`, with both backend and frontend running:

```powershell
npm install
npx playwright install chromium
npm test
```

## Inspecting reports and evidence

- Newman: pass `--reporters cli,htmlextra` (see CI workflow) to get an HTML report under
  `reports/newman/`.
- Playwright: `npm run report` from `automation/playwright/`, or open
  `reports/playwright-html-report/index.html` directly.
- CI: every workflow run uploads the Newman report, Playwright HTML report, Playwright
  traces/screenshots, and app logs as workflow artifacts — downloadable from the Actions run
  page even when the run failed.
- Defects: `documentation/defect-report.md`, cross-referenced to `bug-reports/evidence/`.

## Test coverage summary

See [`documentation/test-summary.md`](documentation/test-summary.md) for real, executed metrics
(pass/fail/blocked/not-run counts, confirmed defects, and limitations) — not estimates.

## Known limitations / out of scope

See `documentation/requirements-specification.md` ("Out of scope") and
`documentation/test-summary.md` ("Known limitations") for the full list — in short: no
pagination, no roles/multi-tenancy, no password reset, no formal load/performance testing, no
full penetration test, and the AUT itself is a purpose-built demo rather than a pre-existing
production system.

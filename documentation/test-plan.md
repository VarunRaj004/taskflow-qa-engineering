# TaskFlow QA — Test Plan

## 1. Objective

Verify that the TaskFlow Demo AUT (`aut-demo/`) behaves correctly against the requirements in
[`requirements-specification.md`](requirements-specification.md), across manual functional
testing, REST API testing, database validation and automated end-to-end UI testing, and to
produce a reproducible, CI-executed test suite as a QA engineering portfolio artifact.

## 2. Scope

**In scope:** authentication (register/login/me/logout), task CRUD, filtering/search/sort,
ownership/access control, dashboard counts, the non-functional checks listed in
`requirements-specification.md` §NFR.

**Out of scope:** anything listed under "Out of scope" in `requirements-specification.md`;
formal load/performance testing; a full penetration test; cross-browser coverage beyond
Chromium; mobile viewports.

## 3. Test items

- Backend: `aut-demo/backend` (Spring Boot REST API)
- Frontend: `aut-demo/frontend` (React SPA)
- Database: MySQL 8 schema created by the backend (`aut-demo/docker-compose.yml`), validated
  directly with SQL in `database-testing/`

## 4. Test levels and tools

| Level | Tool | Where |
|---|---|---|
| Manual functional / negative / boundary / UI | Human execution against a running local build | `documentation/test-cases.csv`, `documentation/test-execution-report.md` |
| API | Postman collection + Newman | `api-testing/` |
| Database | Parameterized SQL against a local/test MySQL instance | `database-testing/` |
| Automated UI / E2E | Playwright (JavaScript) | `automation/playwright/` |
| Regression | Re-run of the above after any fix | `documentation/regression-test-report.md` |
| CI | GitHub Actions | `.github/workflows/qa-tests.yml` |

## 5. Environment

- Local: `aut-demo/backend` run with Spring profile `dev` against MySQL started via
  `docker compose up -d` in `aut-demo/`, or Spring profile `test` against embedded H2 for
  dependency-free checks; `aut-demo/frontend` run via `npm run dev` (Vite).
- CI: GitHub Actions `ubuntu-latest` runner, MySQL as a service container, backend/frontend
  started in the workflow, Playwright run headless with the installed Chromium browser.
- Test data: synthetic accounts created per test run (e.g. `qa.alice+<run-id>@example.test`),
  never real emails or production data. See `database-testing/README.md` for cleanup.

## 6. Entry / exit criteria

**Entry:** AUT builds and starts locally (backend `mvn spring-boot:run`, frontend
`npm run dev`); database reachable; `API_CONTRACT.md` finalized as the requirements source.

**Exit:** all applicable items in the specification's "Definition of done" are met or explicitly
documented as blocked/limited in `test-summary.md`; no test marked Pass without having actually
been executed; every Fail has a defect entry or a documented reason it is not a confirmed defect.

## 7. Roles

Single-person QA engineering exercise: requirements analysis, test design, execution, automation
and defect documentation all performed by the same contributor, acting in the QA engineer role
against a demo AUT built for this purpose.

## 8. Risks and assumptions

- The AUT was purpose-built for this exercise (no pre-existing production app was available),
  which is disclosed; test design still follows the same discipline as it would against a real
  third-party AUT (derive from source/contract, don't assume).
- Because a single session built both the AUT and the QA suite, defects are confirmed only by
  independently re-running the actual request/UI action and observing the actual response — the
  AUT's source is not read as a shortcut to "already know" the answer when a test is executed.
- Time-boxed to a single build session rather than the specification's illustrative 14-day plan;
  §"Known limitations" in `test-summary.md` records anything reduced in scope as a result.

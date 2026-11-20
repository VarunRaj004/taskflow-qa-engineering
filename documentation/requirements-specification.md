# TaskFlow — Requirements Specification

**Application under test (AUT):** TaskFlow Demo — a synthetic React + Spring Boot + MySQL
task-management application built specifically for this QA portfolio project (see
[`aut-demo/`](../aut-demo) and its [API contract](../aut-demo/API_CONTRACT.md), which is the
authoritative source for every requirement below). No pre-existing task-management app was
available to test against, so a minimal but fully functional one was built to exercise the QA
process end-to-end. This is disclosed in the top-level README and is not presented as a
third-party or production system.

Each requirement has a stable ID so it can be traced to test scenarios and test cases
(`test-cases.csv`). IDs are never reused or renumbered once assigned.

## REQ-AUTH — Authentication

| ID | Requirement |
|---|---|
| REQ-AUTH-001 | A visitor can register with name, email and password; the account is created and a 201 response returned. |
| REQ-AUTH-002 | Registration rejects a missing/blank name, a malformed email, or a password under 8 characters, with a 400 and a human-readable error message. |
| REQ-AUTH-003 | Registration rejects an email that is already registered (case-insensitive) with a 409. |
| REQ-AUTH-004 | A registered user can log in with correct email + password and receives a JWT bearer token. |
| REQ-AUTH-005 | Login with an unknown email or a wrong password is rejected with a 401 and an identical, non-revealing error message in both cases. |
| REQ-AUTH-006 | Login rejects missing email/password fields with a 400. |
| REQ-AUTH-007 | An authenticated request to `/api/users/me` returns the caller's own identity. |
| REQ-AUTH-008 | A request to any protected endpoint without a token, with a malformed token, or with an expired/invalid token is rejected with a 401. |
| REQ-AUTH-009 | The frontend redirects an unauthenticated visitor away from `/dashboard` and `/tasks` to `/login`. |
| REQ-AUTH-010 | "Log out" clears the stored session and returns the user to an unauthenticated state. |

## REQ-TASK — Task management

| ID | Requirement |
|---|---|
| REQ-TASK-001 | An authenticated user can create a task with a title (required) and optional description, status and priority. |
| REQ-TASK-002 | Task creation rejects a blank/whitespace-only title, a title over 200 characters, or a description over 2000 characters, with a 400. |
| REQ-TASK-003 | Task creation rejects an invalid `status` or `priority` enum value with a 400. |
| REQ-TASK-004 | A newly created task defaults to status `TODO` and priority `MEDIUM` when not supplied. |
| REQ-TASK-005 | An authenticated user can list their own tasks via `GET /api/tasks`. |
| REQ-TASK-006 | The task list can be filtered by `status` and by `priority`. |
| REQ-TASK-007 | The task list can be filtered by a `search` term matched against title/description. |
| REQ-TASK-008 | The task list can be sorted by `createdAt`, `title`, `priority` or `status`, ascending or descending. |
| REQ-TASK-009 | An authenticated user can view a single task they own via `GET /api/tasks/{id}`. |
| REQ-TASK-010 | An authenticated user can update a task they own (title/description/status/priority) via `PUT /api/tasks/{id}`. |
| REQ-TASK-011 | An authenticated user can delete a task they own via `DELETE /api/tasks/{id}`; a repeat delete of the same id returns 404. |
| REQ-TASK-012 | Task updates reject an invalid `status`/`priority` enum value, and a title/description that violates the length limits, with a 400. |
| REQ-TASK-013 | A task created, edited or deleted through the UI is reflected correctly on refresh (persistence, not just in-memory UI state). |
| REQ-TASK-014 | An empty task list (no tasks, or no tasks matching the active filter) is shown as a clear empty state, not an error. |

## REQ-ACCESS — Access control / ownership

| ID | Requirement |
|---|---|
| REQ-ACCESS-001 | A user can only see their own tasks in `GET /api/tasks` — another user's tasks never appear. |
| REQ-ACCESS-002 | `GET /api/tasks/{id}` for a task owned by another user returns 404 (not 403), identical to a non-existent id, so existence of other users' tasks is never revealed. |
| REQ-ACCESS-003 | `PUT /api/tasks/{id}` and `DELETE /api/tasks/{id}` enforce the same ownership rule as REQ-ACCESS-002. |
| REQ-ACCESS-004 | No endpoint response ever includes another user's `passwordHash` or any password material. |

## REQ-DASH — Dashboard

| ID | Requirement |
|---|---|
| REQ-DASH-001 | `GET /api/dashboard` returns total task count and counts per status (`todo`, `inProgress`, `done`) for the authenticated user's own tasks only. |
| REQ-DASH-002 | Dashboard counts update after a task is created, deleted, or has its status changed. |
| REQ-DASH-003 | Dashboard counts are internally consistent with the task list (`todo + inProgress + done = total`). |
| REQ-DASH-004 | A user with zero tasks sees a dashboard showing all counts as zero, not an error. |

## Non-functional requirements (checked, not formally load/pen-tested — see `test-summary.md` limitations)

| ID | Requirement |
|---|---|
| REQ-NFR-001 | Error responses never expose a stack trace, SQL text, or an internal file path. |
| REQ-NFR-002 | The application is tested against Chromium (Playwright default channel); no cross-browser claim is made beyond that. |
| REQ-NFR-003 | API response time for the endpoints under test is recorded and compared against a documented, modest threshold (informational, not a formal performance test). |

## Out of scope

The following are explicitly **not** implemented in the demo AUT and are therefore out of scope
for this QA project (recorded here, not invented as missing "defects"):

- Password reset / email verification
- Roles or multi-tenant permissions beyond single-owner task isolation
- Pagination of the task list
- Refresh tokens / "remember me"
- Any payment, billing or third-party integration

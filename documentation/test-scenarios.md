# TaskFlow QA — Test Scenarios

High-level scenarios, grouped by module and mapped to requirement IDs. Each scenario expands
into one or more detailed test cases in `test-cases.csv`.

## Authentication

- S-AUTH-01 — Register with fully valid details succeeds (REQ-AUTH-001)
- S-AUTH-02 — Register with each kind of invalid input is rejected with a clear message (REQ-AUTH-002)
- S-AUTH-03 — Register with an email already in use is rejected, including a differently-cased duplicate (REQ-AUTH-003)
- S-AUTH-04 — Login with correct credentials succeeds and returns a usable token (REQ-AUTH-004)
- S-AUTH-05 — Login with wrong password / unknown email is rejected with a non-revealing message (REQ-AUTH-005)
- S-AUTH-06 — Login with missing fields is rejected (REQ-AUTH-006)
- S-AUTH-07 — `/api/users/me` returns the caller's own identity when authenticated (REQ-AUTH-007)
- S-AUTH-08 — Protected endpoints reject missing/malformed/invalid tokens (REQ-AUTH-008)
- S-AUTH-09 — Unauthenticated UI access to protected routes redirects to login (REQ-AUTH-009)
- S-AUTH-10 — Logout ends the session and returns the user to a logged-out state (REQ-AUTH-010)

## Task management

- S-TASK-01 — Create a task with only the required field (title) and verify the documented defaults (REQ-TASK-001, 004)
- S-TASK-02 — Create a task with all fields populated (REQ-TASK-001)
- S-TASK-03 — Reject invalid create payloads: blank title, over-length title/description, invalid enum (REQ-TASK-002, 003)
- S-TASK-04 — List, view, filter by status, filter by priority, search, and sort tasks (REQ-TASK-005–008)
- S-TASK-05 — View a single owned task by id (REQ-TASK-009)
- S-TASK-06 — Edit a task's fields and verify persistence after refresh (REQ-TASK-010, 013)
- S-TASK-07 — Reject invalid update payloads (REQ-TASK-012)
- S-TASK-08 — Delete a task, verify it is gone, and verify a repeat delete is handled cleanly (REQ-TASK-011)
- S-TASK-09 — Empty / no-match task list shows a clear empty state (REQ-TASK-014)

## Access control

- S-ACC-01 — User A cannot see User B's tasks in the list endpoint (REQ-ACCESS-001)
- S-ACC-02 — User A requesting User B's task by id gets the same response as a non-existent id (REQ-ACCESS-002)
- S-ACC-03 — User A cannot update or delete User B's task (REQ-ACCESS-003)
- S-ACC-04 — No response body ever contains password material (REQ-ACCESS-004)

## Dashboard

- S-DASH-01 — Dashboard counts match the task list for a user with several tasks across statuses (REQ-DASH-001, 003)
- S-DASH-02 — Dashboard counts update immediately after create/delete/status-change (REQ-DASH-002)
- S-DASH-03 — Dashboard for a brand-new user with no tasks shows all-zero, not an error (REQ-DASH-004)

## Cross-cutting

- S-NFR-01 — Error responses across all endpoints are checked for leaked internals (REQ-NFR-001)
- S-NFR-02 — Response time of the core endpoints is recorded (informational) (REQ-NFR-003)
- S-EXP-01 — Exploratory: rapid repeated submits, browser back-button after logout, concurrent filter changes

# TaskFlow Demo AUT — API Contract (source of truth)

This app is a **synthetic demo Application Under Test (AUT)**, built solely so the TaskFlow QA
engineering project has a real, running system to test against. It is not a product and is not
part of the CV claim — the QA work (tests, automation, CI, reports) is the actual deliverable.

Stack: React (Vite) frontend, Spring Boot 3 / Java 21 backend, MySQL 8 (docker-compose for
dev/CI), H2 in MySQL-compatibility mode for the `test` Spring profile (so unit/integration tests
run without a live MySQL server).

## Data model

### User
- id: Long, PK, auto-increment
- name: String, required, max 100
- email: String, required, unique, valid email format
- passwordHash: String (bcrypt), never returned in any response
- createdAt: timestamp

### Task
- id: Long, PK, auto-increment
- title: String, required, 1–200 chars (not blank/whitespace-only)
- description: String, optional, max 2000 chars
- status: enum [TODO, IN_PROGRESS, DONE], default TODO
- priority: enum [LOW, MEDIUM, HIGH], default MEDIUM
- ownerId: Long, FK -> User.id
- createdAt, updatedAt: timestamp

## Auth

JWT bearer token (`Authorization: Bearer <token>`), 24h expiry, HS256, secret from
`JWT_SECRET` env var (never committed). No refresh tokens (out of scope for the demo).

## Endpoints

### POST /api/auth/register
Body: `{ "name": string, "email": string, "password": string }`
- 201 -> `{ "id", "name", "email", "createdAt" }`
- 400 -> `{ "error": string }` — missing/blank name, malformed email, password < 8 chars
- 409 -> `{ "error": "Email already registered" }` — duplicate email (case-insensitive)

### POST /api/auth/login
Body: `{ "email": string, "password": string }`
- 200 -> `{ "token": string, "user": { "id", "name", "email" } }`
- 400 -> `{ "error": string }` — missing fields
- 401 -> `{ "error": "Invalid email or password" }` — unknown user or wrong password (same message
  for both, so the endpoint does not reveal which emails are registered)

### GET /api/users/me
Header: `Authorization: Bearer <token>`
- 200 -> `{ "id", "name", "email", "createdAt" }`
- 401 -> `{ "error": "Unauthorized" }` — missing, malformed, or expired/invalid token

### GET /api/tasks?status=&priority=&search=&sortBy=&sortDir=
- 200 -> `{ "tasks": [ {id,title,description,status,priority,ownerId,createdAt,updatedAt}, ... ], "count": n }`
- Only returns tasks owned by the authenticated user (no cross-user visibility)
- `status`/`priority` filter by exact enum match (400 if an invalid enum value is supplied)
- `search` matches title/description case-insensitively (substring)
- `sortBy` one of `createdAt|title|priority|status` (default `createdAt`), `sortDir` `asc|desc` (default `desc`)
- 401 if no/invalid token

### POST /api/tasks
Body: `{ "title": string, "description"?: string, "status"?: enum, "priority"?: enum }`
- 201 -> created task (owner = authenticated user)
- 400 -> missing/blank title, title > 200 chars, description > 2000 chars, invalid enum value
- 401 if no/invalid token

### GET /api/tasks/{id}
- 200 -> task, only if owned by the authenticated user
- 404 -> task does not exist, **or** exists but is owned by someone else (identical response in
  both cases, so the endpoint does not leak the existence of other users' tasks)
- 401 if no/invalid token

### PUT /api/tasks/{id}
Body: any subset of `{ "title", "description", "status", "priority" }`
- 200 -> updated task
- 400 -> invalid enum value, blank title if title is being set, title/description too long
- 404 -> not found or not owned (same as GET)
- 401 if no/invalid token

### DELETE /api/tasks/{id}
- 204 -> deleted (hard delete)
- 404 -> not found or not owned; also returned on a **repeat delete** of the same id
- 401 if no/invalid token

### GET /api/dashboard
- 200 -> `{ "total": n, "todo": n, "inProgress": n, "done": n }` for the authenticated user's own
  tasks only
- 401 if no/invalid token

## Error body shape (all error responses)
`{ "error": "<human-readable message>" }` — no stack traces, no SQL, no internal paths.

## Frontend routes & accessible contract (for Playwright `getByRole`/`getByLabel`)

- `/register` — fields labelled "Name", "Email", "Password"; submit button role=button name
  "Register". Shows inline validation/error text from the API's `error` message.
- `/login` — fields labelled "Email", "Password"; submit button role=button name "Log in". Shows
  inline error text from the API's `error` message on 401/400.
- `/dashboard` (protected) — heading "Dashboard"; shows Total/To Do/In Progress/Done counts as
  labelled stats.
- `/tasks` (protected) — heading "Tasks"; "New Task" button opens a form (fields "Title",
  "Description", "Status", "Priority", submit button "Save Task"); each task row has buttons
  "Edit" and "Delete" (name includes the task title, e.g. `Delete task: Buy milk`, for
  unambiguous `getByRole`); a "Search" textbox and "Status" / "Priority" filter selects.
- Unauthenticated visits to `/dashboard` or `/tasks` redirect to `/login`.
- A top nav bar has a "Log out" button (role=button) visible only when authenticated.

## Known intentional rough edges (left in for the QA process to find — do not "fix" silently)

Do not treat this section as a shortcut: the QA deliverable must independently derive and
reproduce these from actual behaviour, not copy this list into the defect register verbatim.
Implementers of the backend/frontend forks: implement the app in good faith; do not deliberately
reference this list while coding. (This section is for the tester role only, added after
implementation.)

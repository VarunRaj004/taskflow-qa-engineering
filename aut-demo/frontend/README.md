# TaskFlow Frontend

A small React (Vite) frontend for the TaskFlow demo task-management app. It talks to the
TaskFlow backend over its REST API (see `../API_CONTRACT.md`) and provides registration, login,
a dashboard, and task management.

## Prerequisites

- Node.js 18+ and npm
- The TaskFlow backend running and reachable (see the backend's own README for setup)

## Setup

Install dependencies:

```
npm install
```

Copy the example environment file and adjust the backend URL if needed:

```
cp .env.example .env
```

`.env` sets `VITE_API_BASE_URL`, which defaults to `http://localhost:8080` if unset.

## Running

Start the dev server:

```
npm run dev
```

The app serves on `http://localhost:5173` by default.

## Building

Create a production build:

```
npm run build
```

Output is written to `dist/`. Preview the production build locally with `npm run preview`.

## Routes

- `/register` — Create an account with a name, email, and password. On success, redirects to
  `/login` with a confirmation message. Validation and conflict errors from the API (e.g. a
  duplicate email) are shown inline on the form.
- `/login` — Sign in with email and password. On success, the returned token and user are stored
  in `localStorage` and the app redirects to `/dashboard`. Invalid credentials are shown inline.
- `/dashboard` (requires login) — Shows the signed-in user's task counts: Total, To Do, In
  Progress, and Done, fetched from the backend.
- `/tasks` (requires login) — Lists the signed-in user's tasks with a search box and status/
  priority filters. A "New Task" button reveals a form for creating a task; each task row has
  "Edit" and "Delete" buttons that act on that specific task. The same form is reused, pre-filled,
  when editing.

Visiting `/dashboard` or `/tasks` while signed out redirects to `/login`. A top navigation bar
with a "Log out" button is shown on both protected pages and is not rendered when signed out. If
the backend responds with a 401 (e.g. an expired token) while on a protected page, the stored
session is cleared and the app redirects to `/login`.

# API testing (Postman / Newman)

`postman-collection.json` covers every endpoint in [`../aut-demo/API_CONTRACT.md`](../aut-demo/API_CONTRACT.md):
register/login/me, task CRUD, filtering/search/sort, dashboard counts, and cross-user
access-control isolation (two synthetic users, "User A" / "User B").

## Running locally

With the backend running (see the top-level README):

```powershell
npx newman run postman-collection.json -e postman-environment.example.json --env-var baseUrl=http://localhost:8090
```

(adjust `baseUrl` to whatever port your backend actually started on). `postman-environment.example.json`
contains only placeholder values — no real credentials are stored in either file; each run
generates its own unique synthetic emails via collection pre-request scripts.

For an HTML report (same as CI produces):

```powershell
npm install -g newman-reporter-htmlextra
npx newman run postman-collection.json -e postman-environment.example.json `
  --env-var baseUrl=http://localhost:8090 `
  --reporters cli,htmlextra --reporter-htmlextra-export ../reports/newman/report.html
```

## What each folder checks

1. **Auth** — register (valid/duplicate/malformed email/weak password/missing name), login
   (valid/wrong password/unknown email/missing fields — and asserts the 401 message is
   identical for "wrong password" and "unknown email" so the API doesn't reveal which emails
   are registered), `/api/users/me` (valid/no token/malformed token).
2. **Tasks** — create (minimal/full/blank title/over-length title/invalid enum/no auth), list
   (default/filter by status/filter by priority/search/sort), get by id (owned/non-existent),
   update (valid/invalid enum/blank title), delete (valid/repeat delete).
3. **Dashboard** — counts (consistency check) and the no-auth case.
4. **Access control** — User B creates a task; asserts User A's list never includes it, and
   that User A's GET/PUT/DELETE against User B's task id all return 404 (not 403), matching
   the contract's "don't leak existence of other users' data" requirement.

A few assertions are written to the **contract's intended behaviour** (case-insensitive
search; update rejecting a blank title the same way create does) rather than to whatever the
current implementation happens to do — see `documentation/defect-report.md` for what those two
specific assertions found when actually run.

## Secrets

No real secrets are stored anywhere in this folder. `postman-environment.example.json` is safe
to commit as-is; if you ever add a non-placeholder value, copy it to a local, gitignored
`postman-environment.json` instead and pass that with `-e` in its place.

# Database validation

Target: the local MySQL instance started by `docker compose up -d` from `aut-demo/` (see
`aut-demo/docker-compose.yml` and `aut-demo/.env.example`). Database name `taskflow`, matching
what the backend's `dev`/`docker` Spring profiles connect to.

**Never point these queries at a production database.** There is no production database for
this project — only the local/test MySQL container — but the same discipline applies as it
would against a real AUT.

## Running the queries

```powershell
docker exec -it taskflow-mysql mysql -u root -p"$env:MYSQL_ROOT_PASSWORD" taskflow
```

(or any MySQL client pointed at `localhost:3306`, database `taskflow`, using the credentials in
your local `aut-demo/.env`). Then run the statements in [`validation-queries.sql`](validation-queries.sql)
one at a time, substituting the `:placeholder` values for real data created through the API
during a test pass (see `documentation/test-execution-report.md` for which run this was captured
against).

## What these queries check

See the comments in `validation-queries.sql` — schema sanity, password hashing, registration
persistence, task creation/update/delete persistence, ownership/orphan-row checks, and
dashboard-count consistency against raw rows.

## Evidence capture

Captured output is kept, redacted, in `documentation/test-execution-report.md` under the
Database section — `password_hash` values are redacted to `$2a$...<redacted>` and any email is
replaced with a synthetic `qa.*@example.test` placeholder (which is already all we ever insert).

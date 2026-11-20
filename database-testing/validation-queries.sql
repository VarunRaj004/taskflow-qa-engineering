-- TaskFlow QA — database validation queries
--
-- Target: the local/test MySQL instance started by `docker compose up -d` in aut-demo/
-- (database name "taskflow", per aut-demo/.env.example). NEVER point these at a production
-- database. All queries are parameterized (named placeholders below, substitute via your SQL
-- client's prepared-statement mechanism or Newman/Node's mysql2 placeholders) — no string
-- concatenation of untrusted values.
--
-- Schema (as created by Hibernate ddl-auto=update from the entities in aut-demo/backend):
--   users(id BIGINT PK, name VARCHAR(100) NOT NULL, email VARCHAR(255) NOT NULL UNIQUE,
--         password_hash VARCHAR(255) NOT NULL, created_at DATETIME NOT NULL)
--   tasks(id BIGINT PK, title VARCHAR(200) NOT NULL, description VARCHAR(2000),
--         status ENUM(...) NOT NULL, priority ENUM(...) NOT NULL, owner_id BIGINT NOT NULL,
--         created_at DATETIME NOT NULL, updated_at DATETIME NOT NULL)

-- ============================================================================
-- DV-01 — Schema sanity: confirm the tables and key columns exist as expected
-- ============================================================================
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_KEY
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'taskflow' AND TABLE_NAME IN ('users', 'tasks')
ORDER BY TABLE_NAME, ORDINAL_POSITION;

-- DV-01b — Confirm whether a real foreign-key constraint links tasks.owner_id -> users.id
-- (the entity mapping does not declare a JPA @ManyToOne/@JoinColumn, so this is expected to
-- return zero rows; see documentation/test-summary.md limitations for what that means)
SELECT CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'taskflow' AND TABLE_NAME = 'tasks' AND REFERENCED_TABLE_NAME IS NOT NULL;

-- ============================================================================
-- DV-02 — Registration persists a user with a bcrypt hash, never a plaintext password
-- ============================================================================
-- Run after registering a synthetic user through the API (see test-cases TC-AUTH-001/REG).
SELECT id, name, email, password_hash, created_at
FROM users
WHERE email = :testEmail;
-- Expected: exactly one row; password_hash starts with "$2a$"/"$2b$" (bcrypt), is not equal to
-- the plaintext password used at registration, and created_at is set.

-- DV-02b — Duplicate email is never persisted twice (case-insensitive)
SELECT COUNT(*) AS duplicate_count
FROM users
WHERE LOWER(email) = LOWER(:testEmail);
-- Expected: 1, even if a second registration attempt with different casing was rejected by the API.

-- ============================================================================
-- DV-03 — A created task is persisted with the exact submitted values and correct ownership
-- ============================================================================
SELECT id, title, description, status, priority, owner_id, created_at, updated_at
FROM tasks
WHERE owner_id = :testUserId
ORDER BY created_at DESC;
-- Expected: one row per task created via the API for that user; title/description/status/
-- priority match exactly what was submitted (or the documented defaults TODO/MEDIUM).

-- ============================================================================
-- DV-04 — An update changes only the intended columns and does not create a duplicate row
-- ============================================================================
SELECT COUNT(*) AS row_count
FROM tasks
WHERE id = :taskId;
-- Expected: exactly 1 (update must not insert a second row for the same id).

SELECT id, title, description, status, priority, updated_at
FROM tasks
WHERE id = :taskId;
-- Run before and after a PUT /api/tasks/{id} call and diff manually: only the fields sent in
-- the request body, plus updated_at, should differ; created_at and owner_id must be unchanged.

-- ============================================================================
-- DV-05 — Deletion behaviour: this AUT performs a hard delete (per API_CONTRACT.md); confirm
-- the row is actually gone, not soft-deleted
-- ============================================================================
SELECT COUNT(*) AS row_count
FROM tasks
WHERE id = :deletedTaskId;
-- Expected: 0 after DELETE /api/tasks/{id} returns 204.

-- ============================================================================
-- DV-06 — Ownership isolation at the data layer: no task row should ever reference a
-- non-existent owner (orphaned task), confirming the application-level ownership checks have
-- something consistent to enforce
-- ============================================================================
SELECT t.id AS orphaned_task_id, t.owner_id
FROM tasks t
LEFT JOIN users u ON u.id = t.owner_id
WHERE u.id IS NULL;
-- Expected: zero rows. A non-zero result here (e.g. after a user row were deleted, which the
-- API never exposes) would indicate referential integrity depends entirely on the application,
-- since DV-01b shows no DB-level FK constraint exists to prevent it.

-- ============================================================================
-- DV-07 — Dashboard counts match a direct count of the underlying rows
-- ============================================================================
SELECT
  COUNT(*) AS total,
  SUM(CASE WHEN status = 'TODO' THEN 1 ELSE 0 END) AS todo,
  SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS in_progress,
  SUM(CASE WHEN status = 'DONE' THEN 1 ELSE 0 END) AS done
FROM tasks
WHERE owner_id = :testUserId;
-- Compare this row-for-row against the GET /api/dashboard response for the same user.

-- ============================================================================
-- Safe cleanup (test data only — never run against anything but the local/test database)
-- ============================================================================
-- Delete only rows created by a specific QA test run, identified by an email convention such
-- as 'qa.%+<run-id>@example.test' used throughout documentation/test-cases.csv.
-- DELETE FROM tasks WHERE owner_id IN (SELECT id FROM users WHERE email LIKE :testEmailPattern);
-- DELETE FROM users WHERE email LIKE :testEmailPattern;

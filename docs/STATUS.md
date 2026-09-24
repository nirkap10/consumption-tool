# Status — 2026-09-24

## Where we are

Step 1 done, on branch `step-1-skeleton`. The app is a Spring Boot 3.5.16 project
(package `com.nirkap.consumption`) with Postgres 16 in `docker-compose.yml`. It
connects to the database on startup and answers `GET /api/v1/ping` with
`{"status":"ok"}`.

No tables yet. Flyway runs on startup but has no migrations to apply.

The JDK blocker is gone: Temurin 21 is installed and Maven uses it.

## Next

**Step 2 — Schema and entities.** Flyway `V1__initial_schema.sql` with the four
tables and the two indexes on `usage_events`, plus JPA entities and repositories.

## Open questions

- Maven wrapper (`mvnw`): would let someone build without installing Maven. Not
  in the plan, so not added. Worth deciding before Step 9's README.
- Nothing blocking the design. The deferred items (transactions, idempotency, auth,
  customers) are all recorded in `DECISIONS.md` with their revisit triggers.

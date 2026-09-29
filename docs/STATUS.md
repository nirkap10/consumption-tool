# Status — 2026-09-29

## Where we are

Steps 1, 2 and 3 done and merged into `main` (PR #1, #2 and #4).

Step 1: Spring Boot skeleton, Postgres in Docker, `GET /api/v1/ping`.

Step 2: four Flyway migrations create the four tables: `V1` organizations (written by Nir),
`V2` users, `V3` usage_events with its two indexes, `V4` grants. Each table has a
JPA entity in `entity/` and a Spring Data repository in `repository/`. The app boots
with `ddl-auto: validate`, so Hibernate has confirmed the entities match the tables.

Step 3: `POST /organizations` and `POST /users`, both returning `201` with `{"id": ...}`.
A new user starts with `remaining_credit = monthly_allowance`. Request bodies are
validated (400), and an unknown `organizationId` returns 404. Checked with curl.

Step 4 is done on branch `step-4-balance`, waiting for Nir to merge:
`GET /users/{userId}/balance` returns `200 {"remainingCredit": N}`. Unknown user
is 404, a malformed id is 400. Checked with curl.

PRs are merged by Nir without waiting for a mentor review (see DECISIONS).

## Next

- **Step 5 — Usage.** `POST /usage`.

## Open questions

- Maven wrapper (`mvnw`): would let someone build without installing Maven. Not
  in the plan, so not added. Worth deciding before Step 9's README.
- `User` has no way to change `remaining_credit` yet. Steps 5, 6 and 8 need one;
  add it in Step 5 when the first caller exists.
- Nothing blocking the design. The deferred items (transactions, idempotency, auth,
  customers) are all recorded in `DECISIONS.md` with their revisit triggers.

# Status — 2026-09-24

## Where we are

Step 2 done, on branch `step-2-schema` (branched from `step-1-skeleton`, because
PR #1 is still waiting for review).

Four Flyway migrations create the four tables: `V1` organizations (written by Nir),
`V2` users, `V3` usage_events with its two indexes, `V4` grants. Each table has a
JPA entity in `entity/` and a Spring Data repository in `repository/`. The app boots
with `ddl-auto: validate`, so Hibernate has confirmed the entities match the tables.

Step 1 (Spring Boot skeleton, Postgres in Docker, `GET /api/v1/ping`) is PR #1.

## Next

- Merge PR #1 after review, then open PR #2 for `step-2-schema`.
- **Step 3 — Organizations and users.** `POST /organizations` and `POST /users`.

## Open questions

- Maven wrapper (`mvnw`): would let someone build without installing Maven. Not
  in the plan, so not added. Worth deciding before Step 9's README.
- `User` has no way to change `remaining_credit` yet. Steps 5, 6 and 8 need one;
  add it in Step 5 when the first caller exists.
- Nothing blocking the design. The deferred items (transactions, idempotency, auth,
  customers) are all recorded in `DECISIONS.md` with their revisit triggers.

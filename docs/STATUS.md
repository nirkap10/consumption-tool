# Status — 2026-10-01

## Where we are

Steps 1 to 8 done and merged into `main` (PR #1, #2, #4, #5, #6, #8, #10 and #11).

Step 1: Spring Boot skeleton, Postgres in Docker, `GET /api/v1/ping`.

Step 2: four Flyway migrations create the four tables: `V1` organizations (written by Nir),
`V2` users, `V3` usage_events with its two indexes, `V4` grants. Each table has a
JPA entity in `entity/` and a Spring Data repository in `repository/`. The app boots
with `ddl-auto: validate`, so Hibernate has confirmed the entities match the tables.

Step 3: `POST /organizations` and `POST /users`, both returning `201` with `{"id": ...}`.
A new user starts with `remaining_credit = monthly_allowance`. Request bodies are
validated (400), and an unknown `organizationId` returns 404. Checked with curl.

Step 4: `GET /users/{userId}/balance` returns `200 {"remainingCredit": N}`. Unknown user
is 404, a malformed id is 400. Checked with curl.

Step 5: `POST /usage` writes the event row (with the user's organization id),
lowers `remaining_credit` via `User.spend`, and returns `201 {"remainingCredit": N}`.
Over-spending drives the balance negative and still succeeds. `occurredAt` defaults
to now. Unknown user is 404; missing fields or `tokensUsed <= 0` are 400. Checked
with curl and in the `usage_events` table.

Step 6: `POST /grants` writes the grant row (with
the user's organization id), raises `remaining_credit` via `User.grant`, and returns
`201 {"remainingCredit": N}`. `reason` is optional; `granted_at` is set by the
database. Unknown user is 404; missing or non-positive `amount` is 400. Checked with
curl and in the `grants` table. Written by Nir, fixed up with Claude.

Step 7: `GET /reports/monthly` with exactly one of `userId` / `organizationId`, and
an optional `month` (`YYYY-MM`, UTC, defaults to the current month). Returns
`month`, `totalTokens` and `byService`; `byUser` only for organization reports.
Neither or both ids, or a bad month, is 400; unknown id is 404. Checked with curl
against seeded events across two users, two services, three months and a second
organization, including events at 23:59:59 and 00:00:00 on the month boundary.

Step 8: a `@Scheduled` job at midnight UTC on the 1st, plus `POST /admin/reset`
(returns `{"usersReset": N}`), sets every `remaining_credit` back to
`monthly_allowance`. Checked with curl: a spent-and-granted balance and a negative
balance both returned to their allowance; usage events were untouched.

Step 9 is done and pushed, **not merged yet** (branch `step-9-readme-tests`; open the PR
at https://github.com/nirkap10/consumption-tool/pull/new/step-9-readme-tests).
The README covers the problem, the two-call flow, the API, how to run it, and a
copy-paste walkthrough in both Bash and Windows PowerShell — both were run as
written and give the numbers shown. `UsageFlowTest` has 5 tests over the usage flow
(starting balance, exact decrement, negative balance, 404, 400); `mvn test` is green.
The tests need the docker-compose Postgres running.

With Step 9 merged, every step in `PLAN.md` is done: **v1 is complete.**

PRs are merged by Nir without waiting for a mentor review (see DECISIONS).

## Next

- Merge Step 9. Then decide what comes after v1 — `PLAN.md` has no steps left.

## Open questions

- Maven wrapper (`mvnw`): would let someone build without installing Maven. Still
  not added; the README tells people to install Maven.
- Two usage reports or grants for the same user at the same moment can lose one
  balance change; the same applies to a usage report arriving during a reset (see DECISIONS 2026-09-29). Revisit together with transactions and idempotency.
- `notes/` (Nir's learning notes, e.g. `notes/step-3-explained.md`) is untracked.
  Decide whether to commit it or add it to `.gitignore`.
- The GitHub CLI (`gh`) is not installed, so Claude cannot open PRs; Nir opens
  them in the browser. `winget install GitHub.cli` + `gh auth login` would fix it.
- Nothing blocking the design. The deferred items (transactions, idempotency, auth,
  customers) are all recorded in `DECISIONS.md` with their revisit triggers.

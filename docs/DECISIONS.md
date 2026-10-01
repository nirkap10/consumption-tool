# Decisions

Append-only. Never edit or delete an entry — if a decision is reversed, add a new
entry that supersedes it. Each entry records what was decided, why, and what was
rejected, so settled questions stay settled.

---

## 2026-09-23 — Advisory model, never a gateway

**Decision:** We record usage and answer balance checks. We never sit between the
employee and the service, and we never block a call. The company's system asks us
before the call and reports to us after, and decides for itself what to do with
our answer.

**Why:** Reliability. A service in the request path becomes a latency and
availability problem for the thing it measures — if we go down, their AI features
go down. A monitor that goes down just misses data points.

**Rejected:** Proxy/gateway in front of the services. Costs far more to build and
makes us a production dependency for every integrating company.

**Consequence:** Enforcement is cooperative — callers are trusted to ask before and
to report honestly after. Acceptable while callers are internal company systems.
Signed reservation tokens are the answer if this ever faces untrusted customers.

---

## 2026-09-23 — Tokens are a generic unit; we do no pricing

**Decision:** A "token" means whatever the organization decides. They set what
their services cost in tokens and tell us how many to charge per call. We store
and report quantities only — no prices, no currency, no conversion.

**Why:** It is the simplest possible core and it makes the tool work for any
service, not just AI. Pricing is the organization's business logic, not ours.

**Rejected:** Pegging credits to USD and computing cost ourselves from per-model
input/output token prices. That requires maintaining a price table that goes stale
every time a provider changes rates, and it only works for LLM services.

**Consequence:** Token columns are plain `BIGINT`. There is no `BigDecimal`
anywhere, because there is no money anywhere.

---

## 2026-09-23 — Monthly allowance with admin grants, no rollover

**Decision:** Each user has a `monthly_allowance`. Admins can add extra credit
through grants. On reset day every user's `remaining_credit` returns to
`monthly_allowance`. All users reset on the same date.

**Why:** Matches how companies actually budget internal services. A single shared
reset date keeps the job trivial.

**Rejected:** Rollover of unused credit — balances grow unbounded unless a cap is
added, which is a second decision and more state. Also rejected: per-user reset
dates, which turn one scheduled job into a scheduling problem.

---

## 2026-09-23 — We generate user ids

**Decision:** `POST /users` returns a UUID we generate. The company's system stores
it and sends it on every subsequent call.

**Why:** We never inherit whatever id format a future customer happens to use, and
our primary key stays uniform forever.

**Rejected:** Using the company's own employee id as our key. Also considered and
deferred: storing their id alongside ours as an `external_id` — worth adding the
day an integrating company objects to storing our UUID.

---

## 2026-09-23 — Raw events are the source of truth

**Decision:** One row per usage in `usage_events`. `remaining_credit` on the user
row is a convenience cache updated on the same write. Monthly reports are computed
with `SUM ... GROUP BY` at read time.

**Why:** The events are auditable and the report can never drift from them.
Pre-aggregation exists to make reports fast over millions of rows; we will not have
millions of rows, and it would add a second thing to keep correct.

**Rejected:** Maintaining per-user-per-month running totals.

**Consequence:** The balance can be reconciled at any time —
`monthly_allowance + sum(grants) - sum(usage)` should equal `remaining_credit`.
Worth exposing as an admin check later.

---

## 2026-09-23 — Balance may go negative

**Decision:** `POST /usage` always records the event and always decrements the
balance, even when there is not enough credit.

**Why:** Usage is reported after the fact. The tokens are already spent — refusing
to record them would mean knowingly storing a false number. The balance-check
endpoint is what prevents overruns, up front.

**Rejected:** Rejecting over-quota usage reports, which loses real data.

---

## 2026-09-23 — Transactional write deferred

**Decision:** Inserting the usage row and decrementing `remaining_credit` are two
separate writes in v1, not one transaction.

**Why:** Deliberate deferral to keep v1 simple. Correctness hardening comes after
the tool works end to end.

**Known risk:** If the second write fails after the first succeeds, the balance is
silently wrong and nothing detects it.

**Revisit when:** Anyone relies on the balance being exact. The fix is one
`@Transactional` annotation on the service method.

---

## 2026-09-23 — Idempotency on /usage deferred

**Decision:** `POST /usage` takes no event id. A retried report is counted twice.

**Why:** Same reasoning as the transaction above — v1 stays minimal.

**Known risk:** A network retry from the company's system silently corrupts the
balance and every report derived from it.

**Revisit when:** Same trigger as the transaction. The fix is one column plus a
unique constraint, and the two should be done together.

---

## 2026-09-23 — No auth in v1

**Decision:** No login, no API keys, no tokens. Every endpoint is open.

**Why:** Out of scope for a first working core, and adding it badly is worse than
not adding it.

**Revisit when:** The tool is exposed anywhere other than localhost.

---

## 2026-09-23 — Customers get their own table later

**Decision:** Only employees exist for now. When customers arrive they get their
own table rather than sharing the `users` table.

**Why:** Keeps today's model simple and avoids designing for a requirement we
cannot yet see clearly.

**Known cost:** Balance, grant and usage logic will need either duplicating for
customers or refactoring into a shared "account" concept. The refactor is the
better answer, and it gets cheaper the earlier it is recognised.

---

## 2026-09-23 — Stack: Java 21 + Spring Boot 3 + PostgreSQL

**Decision:** Java 21, Spring Boot 3.x on Maven, Spring Data JPA, PostgreSQL 16 in
Docker, Flyway for migrations, JUnit 5.

**Why:** The project is REST endpoints over a relational database with
transactional writes — precisely what Spring Boot is built for. Strong enterprise
hiring market, which matters since this project exists partly to support a job
search.

**Rejected:** Python + FastAPI (faster to write, weaker signal for backend roles);
C# + .NET (equivalent, but the local market leans Java).

---

## 2026-09-23 — Fresh start; earlier design abandoned

**Decision:** This project starts from scratch in `consumption-tool`. The earlier
`AI consumption tracking` folder — USD-pegged credits, per-model pricing tables, a
CV Contract, bearer-token auth — is abandoned and nothing is carried over from it.

**Why:** Its core decisions directly contradict this design. It priced usage itself
and explicitly did not build a pre-call balance check; this tool does the opposite
of both. Merging them would mean carrying complexity this project deliberately
rejected.

**Note:** That folder contained no source code, only documents, so nothing was lost.
It is left in place, untouched.

---

## 2026-09-24 — Spring Boot 3.5.16, package `com.nirkap.consumption`

**Decision:** The project inherits from `spring-boot-starter-parent` 3.5.16, the
newest 3.x release at the time. Maven coordinates are `com.nirkap:consumption-tool`
and all code lives under the `com.nirkap.consumption` package.

**Why:** The stack decision says Spring Boot 3.x; 3.5 is the last 3.x line. The
package name replaces IntelliJ's `org.example` placeholder with one tied to the
author's GitHub name.

**Rejected:** Spring Boot 4.x, which is outside the agreed stack. Also rejected:
`io.github.nirkap10`, the formal convention for GitHub-hosted projects, as longer
for no practical gain here.

**Consequence:** Flyway 10+ (brought in by this Spring Boot version) needs the
separate `flyway-database-postgresql` dependency to talk to Postgres.

---

## 2026-09-24 — One migration per table; plain id fields in entities

**Decision:** The schema is four Flyway migrations, one per table, in foreign-key
order: `V1` organizations, `V2` users, `V3` usage_events, `V4` grants. This
supersedes the single `V1__initial_schema.sql` described in `PLAN.md` Step 2.

Entities store related ids as plain fields (`UUID organizationId`, `UUID userId`),
not as JPA relationships (`@ManyToOne Organization organization`). The foreign
keys are still enforced by the database.

The two indexes on `usage_events` are `(user_id, occurred_at)` and
`(organization_id, occurred_at)`, matching the two scopes of the monthly report.

Code is laid out by layer: `controller/`, `entity/`, `repository/` (and `service/`
from Step 3).

**Why:** One file per table let Nir write the first table on its own and keeps each
file small to review. Plain id fields are the simplest mapping: no lazy loading, no
extra queries, and the API already deals in ids.

**Rejected:** JPA relationships — useful when code navigates from a user to its
organization object, which nothing here does yet. Revisit if that changes.

---

## 2026-09-24 — Nir merges each step's PR; no mentor approval per step

**Decision:** Each step still gets its own branch and pull request, but Nir merges
it himself. The mentor does not have to approve every step before it lands on
`main`.

**Why:** Waiting for a review on every step slows the work down, and Nir decided
the per-step approval isn't needed.

**Kept:** One branch and one PR per step. Each step stays a small, readable unit
in the history, and the mentor can still look at any PR afterwards.

---

## 2026-09-28 — Step 3: request DTOs, check-first 404, id-only responses

**Decision:** Request bodies are Java records in a `dto/` package, validated with
annotations (`@NotBlank`, `@NotNull`, `@PositiveOrZero`) so bad input gets a 400
from Spring before our code runs. Business rules live in `service/`.

`POST /users` checks that the organization exists before saving, and answers 404
through Spring's built-in `ResponseStatusException` if it does not. The database
foreign key stays as a safety net underneath.

Both create endpoints return `201 Created` with only `{"id": "..."}`.
`monthlyAllowance` may be 0; negative values are rejected.

**Why:** DTOs keep the API shape separate from the table shape, so callers cannot
set fields we own (`id`, `remaining_credit`, `created_at`). An explicit existence
check reads as the rule it enforces; the id-only response is the minimum Step 3
asks for.

**Rejected:** Catching the database's `DataIntegrityViolationException` and turning
it into a 404. That exception fires for any broken constraint, so telling "unknown
organization" apart means parsing constraint names out of the message. Also
rejected for now: a global exception handler class, and returning the full row.

---

## 2026-09-29 — Step 4: balance returns only remainingCredit

**Decision:** `GET /users/{userId}/balance` returns `200` with
`{"remainingCredit": N}`. An unknown user is 404 (`findById` + `ResponseStatusException`,
same pattern as Step 3); a path id that is not a UUID is rejected by Spring with 400.

**Why:** The caller asks one question, "does this employee have credit?", and this
number answers it. Whether to allow the call is the company's decision, not ours.

**Rejected:** A `hasCredit` boolean (that is us deciding) and returning the whole
user row (more than the question needs). Easy to extend later without breaking v1.

---

## 2026-09-29 — Step 5: usage lowers the balance in Java, then saves

**Decision:** `POST /usage` loads the user (404 if unknown), inserts the
`usage_events` row with the user's `organization_id`, then calls `User.spend(tokens)`
and saves the user. It returns `201 {"remainingCredit": N}`. `occurredAt` defaults to
the current time when not sent. `tokensUsed` must be greater than 0; `serviceName` is
required and at most 255 characters (the column size).

**Why:** The most obvious code to read: load, change the object, save. It matches the
entities-with-plain-fields style and needs no custom query.

**Known risk:** This is read-change-write. If two usage reports for the same user run
at the same moment, both can read the same balance and one decrement is lost. The
event rows stay correct (raw events are the source of truth), only the cached
`remaining_credit` drifts.

**Rejected for now:** An atomic SQL update
(`UPDATE users SET remaining_credit = remaining_credit - :tokens`), which avoids the
lost update but needs a custom `@Modifying` query and a re-read to return the new
balance.

**Revisit when:** Same trigger as the transaction and idempotency entries, and fix
them together.

---

## 2026-09-29 — A step may branch from the previous, unmerged step

**Decision:** When a step needs code from a step whose PR is not merged yet, its
branch is created from that step's branch instead of from `main`. The PRs are merged
in order. Step 5 (`step-5-usage`) was branched from Step 4 (`step-4-balance`) this
way; PR #5 and PR #6 were merged in that order.

**Why:** Keeps one branch and one PR per step without waiting for each merge before
starting the next step.

**Kept:** One branch and one PR per step. Merge order matters: the earlier step
first, so the later PR only shows its own changes.

---

## 2026-09-30 — Step 6: grants mirror usage; the server sets the time

**Decision:** `POST /grants` loads the user (404 if unknown), inserts the `grants`
row with the user's `organization_id`, then calls `User.grant(amount)` and saves the
user. It returns `201 {"remainingCredit": N}`. `amount` must be greater than 0;
`reason` is optional and at most 255 characters. The request has no time field:
`granted_at` is set by `@CreationTimestamp`.

**Why:** Same load-change-save shape as Step 5, so it reads the same. Unlike usage,
a grant is not reported after the fact — the call itself is the grant — so there is
no earlier "real" time for the client to send.

**Known risk:** Same lost-update and no-transaction risk as Step 5 (see 2026-09-29).
Fix them together.

---

## 2026-09-30 — Class names are singular

**Decision:** Controllers, services and DTOs are named after one thing, in the
singular: `GrantController`, `GrantService`, `CreateGrantRequest` — matching
`UserController` and `UsageService`. URL paths stay plural where the API says so
(`/grants`, `/users`).

**Why:** One consistent rule is easier to follow than mixing both. Step 6 was first
written as `GrantsController`/`GrantsService` and renamed before merging.

**Rejected:** Plural class names (`GrantsService`). Nothing wrong with them, but the
existing classes were already singular.

---

## 2026-09-30 — Docs are updated directly on main

**Decision:** Updates to `docs/` (STATUS, DECISIONS, PLAN) are committed and pushed
straight to `main`, without a branch or pull request. This replaces the
`docs-after-step-N` branches used for PR #7 and PR #9.

**Why:** Nir's call — a docs update is not a step and does not need its own PR.

**Kept:** One branch and one PR per code step.

---

## 2026-09-30 — Step 7: report shape, UTC months, exactly one scope

**Decision:** `GET /reports/monthly` returns
`{"month", "totalTokens", "byService": {name: tokens}, "byUser": {userId: tokens}}`.
`byUser` is only present for organization reports (left out of the JSON for user
reports). Exactly one of `userId` / `organizationId` is required — neither or both
is 400. `month` is `YYYY-MM`, parsed by Spring into a `YearMonth`, and defaults to
the current month. Months are calendar months in **UTC**. A month with no usage
returns `totalTokens: 0` and empty maps. Unknown user or organization is 404.

The sums come from three `GROUP BY` queries on `UsageEventRepository`;
`totalTokens` is the sum of `byService`, added up in Java.

**Why:** Maps are the shortest shape to read and build (Nir chose them over lists of
objects). UTC is the one time zone the server can pick without knowing the
company's. Rejecting "both ids" avoids guessing which one the caller meant.

**Rejected:** Lists of `{name, tokens}` objects (easier to extend, longer to read);
a per-organization time zone (would need a new column).

---

## 2026-09-30 — Step 8: reset loads every user, changes it, saves all

**Decision:** The reset loads all users, calls `User.resetCredit()` on each
(`remaining_credit = monthly_allowance`) and saves them with `saveAll`. The
`@Scheduled` job runs at midnight UTC on the 1st (same UTC as the reports) and
`POST /admin/reset` runs the same method on demand, returning `{"usersReset": N}`.
`@EnableScheduling` is on the application class.

**Why:** Same load-change-save pattern as usage and grants; no custom query needed.
Fine at our number of users.

**Known risk:** A usage report or grant for a user that lands while the reset is
running can be overwritten. Same family as the lost-update risk in 2026-09-29.

**Rejected for now:** A single `UPDATE users SET remaining_credit = monthly_allowance`
query — atomic and faster, but needs `@Modifying` plus `@Transactional`, and
transactions are still deferred. `/admin/reset` is open like every other endpoint
(no auth in v1).

---

## 2026-10-01 — Step 9: tests run against the docker-compose Postgres

**Decision:** `UsageFlowTest` is a `@SpringBootTest` with `MockMvc`: it starts the
whole app and sends real HTTP-shaped requests through the controllers, services and
repositories into the same Postgres that `docker compose up -d` starts. Each test
creates its own organization and user, so tests never depend on each other or on
data already in the database. Five tests cover the usage flow: starting balance,
exact decrement, negative balance, unknown user (404), zero tokens (400).

The README walkthrough is given twice — Bash with `curl`, and Windows PowerShell
with `Invoke-RestMethod` — because PowerShell's `curl` is a different command and
its JSON quoting differs.

**Why:** Testing through the real stack and the real database is the most honest
check with the least setup — no new dependencies.

**Known cost:** `mvn test` needs Docker running, and test rows accumulate in the
local database (harmless: every test uses fresh ids).

**Rejected for now:** Testcontainers (a throwaway Postgres per test run — cleaner,
but a new dependency) and an in-memory H2 database (fast, but not Postgres, so it
can pass where Postgres would fail). Revisit Testcontainers if tests ever run in CI.

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

---

## 2026-10-05 — Trustworthy balance: transactions, atomic updates, eventId

**Decision:** `UsageService.record()` and `GrantService.create()` are
`@Transactional`: the event or grant row and the balance change are saved
together or not at all. The balance changes in one SQL `UPDATE`
(`UserRepository.spend()` / `addCredit()`), then is read back with
`findRemainingCredit()`. `User.spend()` and `User.grant()` are removed.

`POST /usage` requires an `eventId`: a string the caller picks, unique per real
usage, at most 255 characters. If that `eventId` is already stored, nothing
changes and the answer is `200 {"remainingCredit": N}`; a new report is `201`.
`V5` adds `usage_events.event_id` with a unique constraint; rows from before it
are `NULL`. The service returns a small `UsageService.Result(remainingCredit,
duplicate)` so the controller can pick the status code.

This supersedes "Transactional write deferred" and "Idempotency on /usage
deferred" (2026-09-23), and the known lost-update risk of Step 5 and Step 6
(2026-09-29, 2026-09-30).

**Why:** Item 1 of the after-v1 list. A failed second write, two requests at the
same moment, or a network retry could each leave `remaining_credit` wrong
without anyone noticing. A retry is not an error, so it gets a success answer:
the caller only needs to know the usage is recorded.

**Rejected:** Answering a duplicate with 409 (the caller would think it failed).
A UUID-typed `eventId` (the id is the caller's, in whatever format it uses). An
optional `eventId` (callers would skip it, and retries would count twice again).
An `eventId` on grants: Nir's call — they are rare admin calls, and a double
grant is visible in the `grants` table.

**Known risk:** Two copies of the same `eventId` arriving at the exact same
moment both pass the existence check; the unique constraint rejects the second
insert, which rolls back and returns 500. The balance stays correct, and the
caller's next retry gets 200. The monthly reset is still load, change, save, so
a usage report or grant during the reset can still be overwritten.

**Breaking:** Every `POST /usage` caller must now send `eventId`, or gets 400.

---

## 2026-10-05 — Grants require an eventId too

**Decision:** `POST /grants` requires an `eventId`, the same way `POST /usage`
does: a string the caller picks, unique per real grant, at most 255 characters.
A repeated `eventId` changes nothing and answers `200 {"remainingCredit": N}`; a
new grant is `201`. `V6` adds `grants.event_id` with a unique constraint; rows
from before it are `NULL`. The `(remainingCredit, duplicate)` answer moved from
`UsageService.Result` to a shared `BalanceResult` record used by both services.

This supersedes the "An `eventId` on grants" rejection in "Trustworthy balance"
(2026-10-05, the entry above).

**Why:** Nir's call. Whether a person or a system sends the grant, if no
response comes back, it will send the same request again. A double grant is as
wrong as a double usage, and spotting it in the `grants` table is not
protection.

**Known risk:** Same as for usage: two copies of the same `eventId` at the exact
same moment give one `201` and one `500` (the unique constraint rejects the
second insert); the balance stays correct.

**Breaking:** Every `POST /grants` caller must now send `eventId`, or gets 400.

---

## 2026-10-13 — Auth postponed

**Decision:** Auth moves from second place to last in PLAN's after-v1 list.
Step 10 (mentor review feedback) comes first.

**Why:** Nir's call. The tool still runs only on localhost, so the trigger in
"No auth in v1" (2026-09-23) has not been reached.

**Known risk:** Unchanged — every endpoint, including `/admin/reset`, is open.

---

## 2026-10-13 — Unplanned small code changes may go straight to main

**Decision:** Work written in `PLAN.md` (steps and after-v1 items) gets its own
branch and pull request. Small code changes that are not in PLAN may be
committed straight to `main`, like docs. Commit `464af38` (an `//idempotent`
comment on `eventId` in two entities) is an example.

**Why:** Nir's call. A branch and PR for a one-line change outside any step is
more ceremony than it is worth.

**Kept:** One branch and one PR per planned step; Nir merges them himself.

---

## 2026-10-13 — The client gives the organization id

**Decision:** `POST /organizations` requires an `id`: the client's own id for
that organization, any string up to 255 characters. We no longer generate it.
The same id is then used as `organizationId` on `POST /users` and on the
monthly report. An id that already exists answers `409 Conflict`.

`V7` changes `organizations.id` and the three `organization_id` columns (users,
usage_events, grants) from `UUID` to `VARCHAR(255)`. The foreign keys are
dropped and re-added around the change; existing UUIDs stay as their text.

**Why:** Step 10, mentor feedback. Our clients already have an id for each
organization they serve; letting them use it with us saves them from storing
and mapping a second id. Any string, for the same reason as `eventId`: the id
is theirs, in whatever format they use.

**Rejected:** Requiring a UUID (smallest change, but most clients' ids are not
UUIDs). Keeping our UUID and adding an `external_id` column (two ids for one
organization). Answering a repeated id with `200` like `eventId` (a different
organization with the same id would be silently ignored).

**Not changed:** User ids are still generated by us (2026-09-23).

**Known risk:** Two creates with the same new id at the exact same moment both
pass the existence check; the primary key rejects the second, which returns
`500`, not `409`.

**Breaking:** Every `POST /organizations` caller must now send `id`, or gets 400.

---

## 2026-10-13 — Logs: one line per write, console only

**Decision:** Each service has an SLF4J `Logger` (already part of Spring Boot,
no new dependency) and writes one line for every change and every refusal:

- `INFO` — organization created, user created, usage recorded, grant recorded,
  a repeated `eventId` (usage or grant) that changed nothing, the monthly reset
  (scheduled start and users reset).
- `WARN` — the 404s and the 409 our code throws: unknown user, unknown
  organization (on user create and on the report), organization id that
  already exists.

Each line carries the ids and numbers needed to follow it (`eventId`, `userId`,
tokens or amount, the new `remainingCredit`). Logs go to the console only,
with Spring Boot's default format.

**Why:** Step 10, Mordi's feedback: whoever runs the tool should be able to see
and trace what happened, e.g. why a balance is what it is. The readers are the
operators, not the API callers — callers only see the HTTP answer.

**Rejected for now:** A log file (needs config and cleanup; console is enough on
localhost). Logging balance checks and reports (reads change nothing and would
flood the log). Logging 400s: Spring rejects bad input before our code runs, so
it needs an exception handler — that is the "catch exceptions" item.

**Known gap:** The lines inside a transaction are written before the commit. If
the commit itself failed, the log would show a write that did not stick; the
error that follows it in the log shows that it did not.

---

## 2026-10-13 — One error handler for every endpoint

**Decision:** `controller/ErrorHandler` (`@RestControllerAdvice`) receives every
exception thrown while handling a request and decides the answer. Every error
body has one shape, `ErrorBody`: `{"status": 404, "error": "User not found: <id>"}`.

- `@Valid` failed → 400, listing each field: `"tokensUsed: must be greater than 0"`.
- Body missing, not JSON, or a value of the wrong type → 400.
- Path or query value of the wrong type (e.g. a `userId` that is not a UUID) → 400.
- Our own `ResponseStatusException` (404, 409, the report's 400) and Spring's
  errors that carry a status (unknown URL, wrong method) → that status and its
  message.
- Anything else → 500 with `"Internal error"`; the full stack trace goes to the
  log as `ERROR`, never into the answer.

Every 4xx is logged once, here, as `WARN` with method, path, status and
message. The `WARN` lines in the services from the logging entry above are
removed; the ids they carried are now in the error messages instead, so the
caller sees them too.

This supersedes "a global exception handler class" in the rejections of
"Step 3: request DTOs, check-first 404, id-only responses" (2026-09-28), and
the `WARN` part of "Logs: one line per write, console only" (2026-10-13).

**Why:** Step 10, Mordi's "catch exceptions". Spring already caught everything,
but with its own default answer: the message was hidden (so a 400 never said
which field was wrong) and 400s left nothing in our log. One class gives
callers a clear reason and gives us a complete log.

**Rejected:** `try/catch` in each controller method (the same code repeated
everywhere). Only setting `server.error.include-message: always` (shows the
message, but logs nothing and keeps Spring's body shape).

**Not changed:** The duplicate-`eventId` race still answers 500 (see 2026-10-05).
The handler could turn it into a 200, but nobody asked for it yet.

---

## 2026-10-13 — Auditing deferred

**Decision:** No auditing for now. Mordi described it as four columns usually
added to every table for security: who created the row and when, and who last
changed it and when (commonly `created_by`, `created_at`, `updated_by`,
`updated_at`; the exact four are to be confirmed with him when it is built).

**Why:** Nir's call. "Who" needs a known caller, and there is no auth yet (see
"Auth postponed", 2026-10-13), so the "by" columns would have nothing real to
hold. Some tables already have a creation time (`created_at`, `occurred_at`,
`granted_at`).

**Revisit when:** Auth is built, or Mordi asks for it before then.

---

## 2026-10-19 — Step 11: reservations in `usage_events`, no new table

**Decision:** A caller may reserve credit before a use and report the real use
after it. This is a second option; plain `POST /usage` works as before.

- `POST /reservations` (`eventId`, `userId`, `serviceName`, `tokens`) takes the
  credit with one `UPDATE ... WHERE remaining_credit >= :tokens`. Changed 1 row →
  a `usage_events` row is written with `tokens_used = tokens` and
  `reserved_until = now + consumption.reservation-minutes` (3, in
  `application.yml`), answer `201`. Changed 0 rows → `409`, nothing written. An
  `eventId` that is already stored → `200`, nothing changes.
- `POST /usage` with that `eventId` confirms it with one `UPDATE ... WHERE
  reserved_until IS NOT NULL`: the row gets the real tokens and the real time,
  `reserved_until` becomes `NULL`, and the balance changes by `real - reserved`
  (back up if less was used, down if more; it may go negative). The service name
  stays the reservation's. Only one call can change the row, so a retry or two
  calls at the same moment are charged once.
- A `@Scheduled` job every 30 seconds deletes each row whose `reserved_until`
  has passed and gives its tokens back, again with a `WHERE reserved_until IS NOT
  NULL` so it cannot touch a row that was just confirmed. A usage call after
  that is normal usage, charged in full. Until the job runs, a late usage call
  still confirms the reservation (up to 30 seconds of grace).
- Monthly reports leave out rows with `reserved_until` set.

**Why:** After-v1 item 5 from Mordi: "allow only if there is enough and
immediately decrease the balance". Nir's design and answers (2026-10-19). Reusing
`usage_events` keeps the change small: one column, one endpoint, one branch in
`UsageService`, one job. It stays advisory: we answer "reserved" or "not enough",
and the company decides what to do; we still never sit in front of the service.

**Rejected:** A `reservations` table (more code; Nir wanted to change little).
`402` for not enough credit (`409` matches our other conflict answer). Counting
waiting reservations in reports (minor and not real use yet).

**Known risk:** The monthly reset is not aware of reservations. A reservation
made before the reset and expiring after it gives its tokens back on top of the
fresh allowance, and a confirmation after the reset charges only the
difference. Nir's call: leave it. Two identical reservations at the exact same
moment behave like the usage race in 2026-10-05: the second insert fails and
rolls back with a 500, and the balance stays correct.

---

## 2026-10-21 — Step 12: a report page in Thymeleaf, no login

**Decision:** The UI part of after-v1 item 6 becomes Step 12: one web page that
shows the monthly report. The viewer is the client, meaning the company that
integrated the tool into its system, not an employee. There is no user login and
no auth. The page is server-rendered with Thymeleaf inside the existing Spring
Boot app. This lifts "any UI" from the deferred list for this one page only.

**Why:** Nir's call (2026-10-21). The report is the only page that matters to
the client. Thymeleaf keeps it to one project, one run, and no JavaScript build,
which fits "simple and basic". Splitting the UI from auth keeps each step small,
and auth's trigger ("No auth in v1", 2026-09-23: exposed anywhere but localhost)
has not been reached.

**Rejected:** A separate React + Vite app (a second project, npm, CORS, and a
lot of new things to learn for one page). Employee or admin logins (the viewer
is the client). Building auth together with the page.

**Known risk:** Like every endpoint today, the page is open: anyone who can
reach the app can read any report. Acceptable on localhost only.

**Revisit when:** The tool runs anywhere but localhost (then auth, PLAN item 7),
or the client needs more than the report.

---

## 2026-10-28 — Step 12 page details: dropdown, tables and CSS bars

**Decision:** The report page lives at `GET /reports`. A dropdown chooses
"User" or "Organization", a text box takes the id, and a month picker takes the
month. The page shows the same numbers as `GET /api/v1/reports/monthly` as
tables plus a bar chart: one bar per service (and per user for an
organization), each bar's length being its share of the month's total. The
bars and the rest of the look are plain CSS inside the page; there is no CSS
library and no JavaScript.

**Why:** Nir chose tables and a chart, picking either a user or an
organization, and left the look to Claude ("light and simple"). Bars drawn with
CSS need no library, no internet connection and no JavaScript, which fits
"simple and basic". The path is ours to choose: locally the page is just
`http://localhost:8080/reports`; a domain only matters once the tool is
deployed (deferred).

**Rejected:** Chart.js (a JavaScript library loaded from the internet, more than
a few bars need). Pico.css or Bootstrap (another dependency for one page).
Choosing "all of the client's organizations" at once (not needed now).

**Revisit when:** The client wants charts a few bars cannot show (e.g. usage
over time), or more than one page.

---

## 2026-10-28 — Step 12 chart: one bar chart of tokens by service

**Decision:** The report page shows one vertical bar chart: services on the x
axis, tokens on the y axis (from 0 up to the biggest service), for both user
and organization reports. The "By service" and "By user" tables are plain
tables with no bars. Still plain CSS, no JavaScript.

This supersedes the bars inside the tables in "Step 12 page details" (2026-10-28,
the entry above).

**Why:** Nir's call after seeing the page: a real chart with axes reads better
than a share bar on each table row.

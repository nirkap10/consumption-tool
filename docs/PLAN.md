# Plan

The roadmap. One branch and one pull request per step, so each step stays small
and separate in the history. Current position lives in `STATUS.md`, not here.

Repo: `https://github.com/nirkap10/consumption-tool.git`

---

## Prerequisite — a working JDK 21

**Resolved 2026-09-24.** Temurin 21.0.12 installed; `java -version` and `mvn -v`
both report 21. The original note is kept below for the record.

**Blocked as of 2026-09-23.** `JAVA_HOME` points at
`C:\Program Files\Eclipse Adoptium\jdk-21.0.8.9-hotspot`, which no longer exists, so
`mvn` fails immediately. The only real JDK on the machine is Java 8, which Spring
Boot 3 will not run on.

Nir runs this himself — installs need a real user, and `JAVA_HOME` must be set
machine-wide:

```
winget install EclipseAdoptium.Temurin.21.JDK
```

Then, in a **new** terminal, both must report 21:

```
java -version
mvn -v
```

Only Step 1 onward needs this. Step 0 is documents and git.

---

## Step 0 — Groundwork

Folder, `git init`, `.gitignore`, `CLAUDE.md`, `docs/PLAN.md`, `docs/DECISIONS.md`,
`docs/STATUS.md`, `README.md` stub. First commit, pushed to `main`.

*Verify:* `git log` shows the initial commit with all files; the push lands on GitHub.

## Step 1 — Skeleton and database

`pom.xml` with the Spring Boot 3 parent and the web, data-jpa, validation,
postgresql, flyway and test dependencies. `docker-compose.yml` running Postgres 16.
`application.yml`. Main class. `GET /api/v1/ping` returning `{"status":"ok"}`.

*Verify:* `docker compose up -d`, `mvn spring-boot:run`,
`curl localhost:8080/api/v1/ping` returns 200.

## Step 2 — Schema and entities

Flyway `V1__initial_schema.sql` creating all four tables plus the two indexes on
`usage_events`. JPA entities and Spring Data repositories.
*(Done as four migrations, one per table — see DECISIONS 2026-09-24.)*

*Verify:* app boots clean; `flyway_schema_history` contains V1; `\dt` in psql lists
four tables.

## Step 3 — Organizations and users

`POST /organizations` and `POST /users`. A new user starts with
`remaining_credit = monthly_allowance`.

*Verify:* curl creates an organization, then a user under it, and gets a UUID back.

## Step 4 — Balance check

`GET /users/{userId}/balance`.

*Verify:* a freshly created user reports `remainingCredit == monthlyAllowance`;
an unknown id returns 404.

## Step 5 — Usage

`POST /usage` writes the event row and decrements `remaining_credit`, returning the
new balance.

*Verify:* reporting 100 tokens drops the balance by exactly 100; over-spending drives
it negative and still succeeds.

## Step 6 — Grants

`POST /grants` writes the grant row and increments `remaining_credit`.
*(Same load-change-save pattern as Step 5, so the same known risk applies — see
DECISIONS 2026-09-29.)*

*Verify:* a grant of 500 raises the balance by 500 and appears in the `grants` table.

## Step 7 — Monthly report

`GET /reports/monthly` with user scope, organization scope, and the month filter.
`byUser` is populated only for organization-scoped reports.

*Verify:* seed several events across two services and two users; hand-check the totals.

## Step 8 — Monthly reset

A `@Scheduled` job setting `remaining_credit = monthly_allowance` for every user on
the 1st, plus `POST /admin/reset` to trigger it manually — otherwise the feature
cannot be tested without waiting a month.

*Verify:* spend some credit, hit the manual trigger, confirm balances are restored.

## Step 9 — README and tests

README covering the problem, the two-call flow, the diagram, how to run it, and a
copy-paste curl walkthrough. A small set of tests over the usage flow.
*(Windows note: in Windows PowerShell `curl` is an alias for `Invoke-WebRequest`,
so the walkthrough must use `curl.exe` or show `Invoke-RestMethod` too.)*

*Verify:* `mvn test` is green; a stranger can follow the README from clone to a
working demo.

---

## End-to-end check, after Step 9

```
docker compose up -d
mvn spring-boot:run

curl -X POST localhost:8080/api/v1/organizations -H 'Content-Type: application/json' \
  -d '{"name":"Acme"}'
curl -X POST localhost:8080/api/v1/users -H 'Content-Type: application/json' \
  -d '{"organizationId":"<org>","monthlyAllowance":1000}'

# the two calls the company's system makes
curl localhost:8080/api/v1/users/<user>/balance                     # 1000
curl -X POST localhost:8080/api/v1/usage -H 'Content-Type: application/json' \
  -d '{"userId":"<user>","serviceName":"chat","tokensUsed":250}'
curl localhost:8080/api/v1/users/<user>/balance                     # 750

curl -X POST localhost:8080/api/v1/grants -H 'Content-Type: application/json' \
  -d '{"userId":"<user>","amount":500,"reason":"extra budget"}'
curl "localhost:8080/api/v1/reports/monthly?organizationId=<org>"   # 250, service "chat"
```

## Step 10 — Mentor review feedback

Comments from the mentor (2026-10-12), as Nir wrote them down. Short term.

- auditing — **deferred** (DECISIONS 2026-10-13): four audit columns per table
- ~~adding unique to eventId~~ — **done** by `V5` / `V6`: the unique constraint
  also creates an index on `event_id`, so the retry check
  (`existsByEventId`) is a fast index lookup
- ~~clients give the organization id~~ — **done**: `POST /organizations` takes
  the client's own `id` (any string), `V7` (DECISIONS 2026-10-13)
- ~~add logs~~ — **done**: one log line per write and per 404/409, console
  only (DECISIONS 2026-10-13)
- ~~catch exceptions~~ — **done**: one `ErrorHandler` for every endpoint, one
  error body shape (DECISIONS 2026-10-13)

## Step 11 — Reservations

A second way to use credit, beside the existing "use, then report": reserve
first, then report the real use (after-v1 item 5; DECISIONS 2026-10-19). No new
table: a reservation is a `usage_events` row written before the use.

- `V8` adds `usage_events.reserved_until` (nullable `TIMESTAMPTZ`). Set = a
  reservation waiting for its usage call; `NULL` = normal usage.
- `POST /reservations` with `eventId`, `userId`, `serviceName`, `tokens`. One
  `UPDATE` takes the credit only if there is enough. Enough → the row is written
  with `reserved_until = now + 3 min`, `201 {"remainingCredit": N}`. Not
  enough → `409`, nothing written. A repeated `eventId` → `200`, nothing changes.
- `POST /usage` with the reservation's `eventId` confirms it: the balance
  changes by `real - reserved`, the row gets the real tokens and
  `reserved_until = NULL`, `201`. A second identical call is a retry (`200`).
- A scheduled job (every 30 seconds) deletes reservations whose time has passed
  and gives their tokens back. A usage call that arrives after that is normal
  usage, charged in full.
- Monthly reports leave out waiting reservations.
- The 3 minutes is a setting in `application.yml`.

*Verify:* reserve 100 of 1000 → 900; confirm with 120 → 880; confirm again →
200 and still 880; reserve 2000 of 1000 → 409; an expired reservation gives its
100 back; a waiting reservation is not in the monthly report.

## Step 12 — Report page

A web page where the client (the company that integrated the tool into its
system) sees the monthly report in a simple design (after-v1 item 6, UI part
only; DECISIONS 2026-10-21). No login and no auth.

- Thymeleaf (`spring-boot-starter-thymeleaf`): Spring Boot renders the HTML
  itself. No JavaScript app, no second project.
- One page, the report. It shows what `GET /api/v1/reports/monthly` already
  returns (`month`, `totalTokens`, `byService`, `byUser`), using the same
  service. No new queries.
- A wrong or unknown id shows a message on the page, not a JSON error.

*To decide when the step starts:* what the client picks on the page (one
organization, one user, or all of its organizations); the page's path; a CSS
library (e.g. Pico.css) or plain CSS; tables only or a chart too.

*Verify:* the app runs, the page opens in a browser and shows the same numbers
as `GET /api/v1/reports/monthly` for the same id and month.

## After v1 — candidates, not scheduled

All nine steps are done. These are the decisions waiting, most important first.
None is a step yet; each becomes one when Nir picks it.

1. ~~**Trustworthy balance**~~ — **done** (PR #13 and #14, DECISIONS 2026-10-05)
   for usage and grants, both with `eventId`. Still open: the monthly reset is not
   yet one atomic update.
2. **Customers** — and with them, whether to refactor users into a shared
   "account" concept (DECISIONS 2026-09-23).
3. **Time zones** — reports and the reset use UTC months; decide whether each
   organization needs its own time zone.
4. **Deployment and CI** — where it runs, plus an automated test run (which would
   bring Testcontainers back; DECISIONS 2026-10-01).
5. ~~**Different credit allowance modes**~~ (mentor, 2026-10-12) — support different
   ways of using credit: "add, save and use" OR "allow only if there is enough
   and immediately decrease the balance". **Now Step 11** (reservations).
6. **Client with auth and a UI** (mentor, 2026-10-12) — create a client (auth) so
   users can see their details in a nice UI. **The UI part is now Step 12**
   (report page, no login). Auth is decided later, with item 7.
7. **Auth** — postponed by Nir (2026-10-13), moved from second place to last.
   Still needed before the tool runs anywhere but localhost; `/admin/reset` is
   open today.

## Out of scope for v1

Auth, transactional writes, idempotency, per-model pricing or USD, customers, any
UI, alerts, forecasting, rate limiting, an SDK, deployment. See `DECISIONS.md`.

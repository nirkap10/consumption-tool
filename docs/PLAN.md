# Plan

The roadmap. One branch and one pull request per step, so the mentor reviews each
one in isolation. Current position lives in `STATUS.md`, not here.

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

## Out of scope for v1

Auth, transactional writes, idempotency, per-model pricing or USD, customers, any
UI, alerts, forecasting, rate limiting, an SDK, deployment. See `DECISIONS.md`.

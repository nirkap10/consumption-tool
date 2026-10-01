# Consumption Tool

Tracks how much of a company's services its employees consume, and answers whether
an employee still has credit left.

## The problem

A company offers its employees internal services (an AI chat, a search tool,
anything) and gives each employee a monthly budget. Someone has to keep count: how
much has each employee used, how much is left, and what did the company spend its
budget on this month. This tool is that counter.

## How it works

It is **advisory, never a gateway**. It does not sit between the employee and the
service. The company's own system makes two calls:

```
Company's system ──(1) GET balance ──► Consumption Tool
       |                                (Spring Boot + Postgres)
       ├──(2) calls the service itself ──► (not involved)
       |
       └──(3) POST usage ─────────────► Consumption Tool
```

1. **Before** the call: "does this employee have credit?" — we answer with a number.
2. The company calls its service itself. We are not involved.
3. **After** the call: "this employee used N" — we record it.

The company decides what to do with our answer — this tool never blocks a call.
Usage is always recorded, even when it takes the balance below zero, because by
then it has already happened.

A **token** is a generic unit. Each organization decides what its own services cost
in tokens; this tool stores and reports quantities, and does no pricing.

Every user gets a **monthly allowance**. Admins can add extra credit with **grants**.
On the 1st of each month (UTC) every balance resets to the allowance; unused credit
does not roll over.

## API

Base path `/api/v1`. Errors: `400` on bad input, `404` on an unknown id.

| Method | Path | Body / query | Returns |
|---|---|---|---|
| POST | `/organizations` | `name` | `201 {"id"}` |
| POST | `/users` | `organizationId`, `monthlyAllowance` | `201 {"id"}` |
| GET  | `/users/{userId}/balance` | — | `{"remainingCredit"}` |
| POST | `/usage` | `userId`, `serviceName`, `tokensUsed`, optional `occurredAt` | `201 {"remainingCredit"}` |
| POST | `/grants` | `userId`, `amount`, optional `reason` | `201 {"remainingCredit"}` |
| GET  | `/reports/monthly` | `userId` **or** `organizationId`, optional `month` (`YYYY-MM`) | report, see below |
| POST | `/admin/reset` | — | `{"usersReset"}` |

The monthly report (months are UTC; default is the current month):

```json
{
  "month": "2026-10",
  "totalTokens": 1000,
  "byService": { "chat": 700, "search": 300 },
  "byUser":    { "<user1>": 400, "<user2>": 600 }
}
```

`byUser` appears only in organization reports.

## Running it

Needs **JDK 21**, **Maven** and **Docker Desktop** (running).

```
git clone https://github.com/nirkap10/consumption-tool.git
cd consumption-tool
docker compose up -d        # start Postgres 16
mvn spring-boot:run         # start the app on port 8080 (Flyway creates the tables)
```

Check it is up: `curl localhost:8080/api/v1/ping` → `{"status":"ok"}`.

## Walkthrough

Copy-paste these in a **second** terminal while the app runs. Replace `<org>` and
`<user>` with the ids the first two calls return.

> **Windows PowerShell:** type `curl.exe`, not `curl` — in PowerShell `curl` is an
> alias for `Invoke-WebRequest`, which takes different arguments. The JSON quoting
> below is for Git Bash / macOS / Linux; the PowerShell versions follow.

**Bash (Git Bash, macOS, Linux):**

```
# setup
curl -X POST localhost:8080/api/v1/organizations -H 'Content-Type: application/json' \
  -d '{"name":"Acme"}'
curl -X POST localhost:8080/api/v1/users -H 'Content-Type: application/json' \
  -d '{"organizationId":"<org>","monthlyAllowance":1000}'

# the two calls the company's system makes
curl localhost:8080/api/v1/users/<user>/balance                     # 1000
curl -X POST localhost:8080/api/v1/usage -H 'Content-Type: application/json' \
  -d '{"userId":"<user>","serviceName":"chat","tokensUsed":250}'    # 750
curl localhost:8080/api/v1/users/<user>/balance                     # 750

# extra credit, the report, and the monthly reset
curl -X POST localhost:8080/api/v1/grants -H 'Content-Type: application/json' \
  -d '{"userId":"<user>","amount":500,"reason":"extra budget"}'     # 1250
curl "localhost:8080/api/v1/reports/monthly?organizationId=<org>"   # 250, service "chat"
curl -X POST localhost:8080/api/v1/admin/reset                      # back to 1000
curl localhost:8080/api/v1/users/<user>/balance                     # 1000
```

**Windows PowerShell** (`Invoke-RestMethod` parses the JSON for you):

```powershell
$api = "http://localhost:8080/api/v1"
$org  = Invoke-RestMethod -Method Post "$api/organizations" -ContentType "application/json" -Body '{"name":"Acme"}'
$user = Invoke-RestMethod -Method Post "$api/users" -ContentType "application/json" `
          -Body "{`"organizationId`":`"$($org.id)`",`"monthlyAllowance`":1000}"

Invoke-RestMethod "$api/users/$($user.id)/balance"                  # 1000
Invoke-RestMethod -Method Post "$api/usage" -ContentType "application/json" `
  -Body "{`"userId`":`"$($user.id)`",`"serviceName`":`"chat`",`"tokensUsed`":250}"   # 750

Invoke-RestMethod -Method Post "$api/grants" -ContentType "application/json" `
  -Body "{`"userId`":`"$($user.id)`",`"amount`":500,`"reason`":`"extra budget`"}"   # 1250
Invoke-RestMethod "$api/reports/monthly?organizationId=$($org.id)" | ConvertTo-Json
Invoke-RestMethod -Method Post "$api/admin/reset"
Invoke-RestMethod "$api/users/$($user.id)/balance"                  # 1000
```

## Tests

```
docker compose up -d
mvn test
```

The tests start the whole app and talk to the Postgres from docker-compose, so
Docker must be running. They cover the usage flow: starting balance, usage lowering
it, over-spending going negative, unknown user (404) and invalid input (400).

## Not built (on purpose)

Auth, transactional writes, idempotency on `/usage`, pricing or currency, customers,
a UI, alerts, forecasting, rate limiting, an SDK, deployment. Each is recorded in
`docs/DECISIONS.md` with why it was left out and when to revisit it.

## Stack

Java 21 · Spring Boot 3 · PostgreSQL 16 (Docker) · Spring Data JPA · Flyway · Maven · JUnit 5

## Docs

- `docs/PLAN.md` — the roadmap
- `docs/STATUS.md` — where things stand
- `docs/DECISIONS.md` — why it is built this way

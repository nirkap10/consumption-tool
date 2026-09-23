# CLAUDE.md

Project context for Claude Code. Read this before working on anything here.

---

## Scope rule — read this first

**Keep everything simple and basic. No advanced features.** The goal is a small,
correct, flexible core — not an impressive one.

- Do only what the current step asks. Nothing outside it.
- If something seems missing or ambiguous, **ask** instead of inventing it.
- If a "while I'm here" improvement occurs to you, write it in
  `docs/STATUS.md` under Open questions. Do not build it.
- Prefer the smallest implementation that is honest. When choosing between
  clever and obvious, choose obvious.

## What this is

A consumption tool. It tracks how much of a company's services its employees
consume, and answers whether an employee still has credit left.

It is **advisory, never a gateway**. We do not sit between the employee and the
service. The company's own system makes two calls to us:

```
Company's system ──(1) GET balance ──► Consumption Tool
       |                                (Spring Boot + Postgres)
       ├──(2) calls the service itself ──► (we are not involved)
       |
       └──(3) POST usage ─────────────► Consumption Tool
```

We answer "does this employee have credit?" and record "this employee used N".
**The company decides what to do with our answer — we never block anything.**

A "token" is a generic unit. The organization decides what its services cost in
tokens and tells us how many to charge. **We do no pricing and no currency
conversion.** That is why token columns are plain `BIGINT` and there is no
`BigDecimal` in this codebase.

Employees are the only consumers for now. Customers come later.

## Stack

| Concern   | Choice                                  |
|-----------|-----------------------------------------|
| Language  | Java 21                                 |
| Framework | Spring Boot 3.x (Maven)                 |
| Data      | Spring Data JPA / Hibernate             |
| Database  | PostgreSQL 16 via docker-compose        |
| Migrations| Flyway                                  |
| Testing   | JUnit 5                                 |

Standard Spring layering: controller → service → repository. Idiomatic, not clever.

## Data model

Four tables. `organization_id` is stored directly on `usage_events` and `grants`
— deliberate denormalization, so org-wide reports need no join and history
survives a user changing organization.

- **organizations** — `id` UUID PK, `name`, `created_at`
- **users** — `id` UUID PK, `organization_id` FK, `monthly_allowance` BIGINT,
  `remaining_credit` BIGINT, `created_at`
- **usage_events** — `id` BIGSERIAL PK, `user_id` FK, `organization_id` FK,
  `service_name`, `tokens_used` BIGINT, `occurred_at`
- **grants** — `id` BIGSERIAL PK, `user_id` FK, `organization_id` FK,
  `amount` BIGINT, `reason`, `granted_at`

Raw events are the source of truth. `remaining_credit` is a convenience cache
updated on write. Reports are `SUM ... GROUP BY` at read time — no pre-aggregation.

## API surface

Base path `/api/v1`.

| Method | Path | Mandatory | Optional |
|---|---|---|---|
| POST | `/organizations` | `name` | — |
| POST | `/users` | `organizationId`, `monthlyAllowance` | — |
| GET  | `/users/{userId}/balance` | `userId` (path) | — |
| POST | `/usage` | `userId`, `serviceName`, `tokensUsed` | `occurredAt` |
| POST | `/grants` | `userId`, `amount` | `reason` |
| GET  | `/reports/monthly` | one of `userId` / `organizationId` | `month` (`YYYY-MM`) |

`GET` for the balance check because nothing is reserved or mutated. `POST`
wherever a row is written. Errors: 400 on validation, 404 on unknown id.

`POST /usage` **always records**, even with insufficient credit — the usage
already happened, and the balance is allowed to go negative.

## Deliberately deferred — do not add these unasked

Auth · transactional writes · idempotency on `/usage` · per-model pricing or USD ·
customers · any UI · alerts · forecasting · rate limiting · an SDK · deployment.

Each of these has an entry in `docs/DECISIONS.md` with the reasoning and the
trigger to revisit. Read that before proposing one of them.

## Working habits

- Start a session with: *read `docs/STATUS.md` and `docs/PLAN.md`*.
- End a session with: *update `docs/STATUS.md` and `docs/DECISIONS.md`*.
- One branch and one pull request per step in `docs/PLAN.md` — the mentor reviews there.
- `docs/PLAN.md` is the future, `docs/STATUS.md` is the present,
  `docs/DECISIONS.md` is the past. This file is the rules.
- `DECISIONS.md` is append-only. Never edit or delete an entry; supersede it with
  a new one.

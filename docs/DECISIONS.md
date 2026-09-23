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

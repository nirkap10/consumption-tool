# Consumption Tool

Tracks how much of a company's services its employees consume, and answers whether
an employee still has credit left.

It is **advisory, never a gateway**. It does not sit between the employee and the
service. The company's own system makes two calls:

```
Company's system ──(1) GET balance ──► Consumption Tool
       |                                (Spring Boot + Postgres)
       ├──(2) calls the service itself ──► (not involved)
       |
       └──(3) POST usage ─────────────► Consumption Tool
```

We answer "does this employee have credit?" and record "this employee used N".
The company decides what to do with the answer — this tool never blocks a call.

A "token" is a generic unit. Each organization decides what its own services cost
in tokens; this tool stores and reports quantities, and does no pricing.

**Status:** early. See `docs/PLAN.md` for the roadmap, `docs/STATUS.md` for where
things stand, and `docs/DECISIONS.md` for why it is built this way.

## Stack

Java 21 · Spring Boot 3 · PostgreSQL 16 (Docker) · Spring Data JPA · Flyway · Maven

## Running it

Not yet runnable — see `docs/PLAN.md`, Step 1.

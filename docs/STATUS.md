# Status — 2026-09-23

## Where we are

Step 0 done. The repository exists with its four working documents and a README
stub, and the first commit is pushed to `main`.

No code yet. Nothing builds yet — that is expected at this point.

## Next

**Step 1 — Skeleton and database.** `pom.xml` with the Spring Boot 3 parent and
starters, `docker-compose.yml` for Postgres 16, `application.yml`, the main class,
and a `GET /api/v1/ping` endpoint.

**Blocked until the JDK is fixed.** `JAVA_HOME` points at an Eclipse Adoptium path
that no longer exists, and the only JDK on the machine is Java 8. Run
`winget install EclipseAdoptium.Temurin.21.JDK`, open a new terminal, and confirm
that both `java -version` and `mvn -v` report 21. See the prerequisite section in
`PLAN.md`.

## Open questions

- Nothing blocking the design. The deferred items (transactions, idempotency, auth,
  customers) are all recorded in `DECISIONS.md` with their revisit triggers.

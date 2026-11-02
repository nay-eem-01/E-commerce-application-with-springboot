# Senpai Supply backend

Anime merchandise shop for Bangladesh: figures, costumes, bottles, bags. Guest checkout with
cash on delivery, phone-code sign-in, a product page built around a 360 viewer. Spring Boot
3.5 / Java 21, PostgreSQL 15+, modular monolith (seven Maven modules). The storefront design
is concept C ("night market"); the frontend lives in its own repository.

## Start of every session

1. Read `docs/DEV_LOG.md` (where we are / next up / open items) and `docs/ROADMAP.md`.
2. `git status` and check the branch. Follow the `git-workflow` skill (`docs/GIT_WORKFLOW.md`).

## Where things are

- `docs/PLAN.md` - decisions, audit of the old code, architecture and scaling path, open questions
- `docs/SECURITY.md` - threat model, controls, open risks
- `docs/ERD.md` - schema and module ownership; `docs/API.md` - contract for the frontend
- `docs/GIT_WORKFLOW.md` - branches, commits, pushing, PRs
- `docs/ROADMAP.md`, `docs/DEV_LOG.md` - steps and dated log
- `.claude/skills/` - `coding-conventions`, `api-conventions`, `security-checklist`, `git-workflow`
- `.env.example` - every environment variable the app reads
- `scripts/smoke_test.py` - end-to-end check against a fresh development database

## Run and test

```bash
cp .env.example .env            # fill DB_*, JWT_SECRET (openssl rand -base64 64), CORS_ALLOWED_ORIGINS
./mvnw -B verify                # build + unit tests (no database needed)
./mvnw -B package -DskipTests && java -jar ecommerce-api/target/ecommerce-api-0.0.1-SNAPSHOT.jar
                                # run from the repo root (reads ./.env); needs PostgreSQL 15+;
                                # the development profile loads a demo catalog
```

Swagger UI: `/swagger-ui.html` (off in production). Never read, print or commit `.env`.

## Rules that matter most

- Caller identity comes from the token (`CurrentUser`), never from a request body or path.
- Private data that is not yours is 404 via a scoped query; admin actions name their privilege.
- Prices and totals are computed on the server; money is `BigDecimal` via `Money`.
- A module never touches another module's entities or repositories; use its `api` interface or an event.
- Return records, not entities. Messages come from `ErrorCode`; no exception detail in responses.
- Order transitions only through `OrderStateMachine`; schema changes only through a new Flyway file.
- Small conventional commits, no attribution; every fix ships with a regression test; update
  `docs/ROADMAP.md` and `docs/DEV_LOG.md` in the same PR.

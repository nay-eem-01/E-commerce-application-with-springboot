# Dev log

## Where we are

Phase 1 (backend rewrite) is done: seven-module modular monolith on Spring Boot 3.5 /
Java 21 / PostgreSQL, every storefront feature from the concept C design has an API, 91 unit
tests and a 66-check end-to-end script pass against a real database.

## Next up

Phase 2: the storefront in its own repository, following concept C exactly (`docs/ROADMAP.md`).

## Open items

- Rotate the JWT secret and MySQL password that were committed by the old code (`docs/SECURITY.md` S1).
- Decide the pre-order deposit rule before enabling bKash (`docs/PLAN.md` §7 Q1).
- Pick an SMS provider and start sender-ID paperwork (`docs/PLAN.md` R1).
- CI is not set up yet; run `./mvnw -B verify` before every push.

---

## 2026-11-02: Phase 1, backend rewrite

**Done**
- Replaced the single-module tutorial app with `ecommerce-common`, `-user`, `-catalog`,
  `-cart`, `-order`, `-review`, `-api` (structure from delivery_app; conventions from
  delivery_app and AgroTrends).
- Auth: phone OTP sign-in that creates the account; staff password sign-in; 15-minute access
  JWT bound to a server-side session; 30-day single-use refresh tokens with reuse detection;
  rate limits; first admin from env with forced password change.
- Catalog with one option group per product, per-option stock, facts, box items, photos,
  360 spin frames and story; filter/sort/search; admin CRUD.
- Cart + wishlist with guest-cart merge; checkout quote; guest or member orders with
  server-side prices, atomic stock, required Idempotency-Key, COD; order state machine with
  DB-enforced append-only history; guest tracking by order number + phone; verified-buyer reviews.
- Flyway V1 to V5, repeatable dev demo catalog (the prototype's 12 products), `.env.example`.
- Docs: PLAN, SECURITY, ERD, API, GIT_WORKFLOW, ROADMAP, this log; `.claude/skills`.

**Decisions** (full table in `docs/PLAN.md` §2)
- MySQL → PostgreSQL 15+; `ddl-auto=update` → Flyway + validate.
- Phone OTP for shoppers instead of username/password; guest checkout by default.
- COD only; other methods wait for a gateway class (config flag `app.payment.enabled-methods`).
- Images are URLs for now; upload is Phase 3.

**Bugs found by the end-to-end run and fixed before commit (regression tests included)**
- Cart merge/view returned 500 when a line was unavailable: the catalog's `@Transactional`
  marked the caller's transaction rollback-only. `CatalogApi.price` now declares `noRollbackFor`.
- Admin order and user search returned 500 with no filter: a null bound inside
  `upper()/lower()` reaches PostgreSQL as `bytea`. Empty string now means "no filter".

**Breaking API changes**
- Everything. The old endpoints (`/api/public/...`, `/api/cart/products/{id}/quantity/{q}`,
  `/api/order/place/{paymentMethod}`, cookie auth) are gone. The new contract is `docs/API.md`.

**Known limitations**
- Rate limits are in memory (one instance). No CI yet. Integration tests are a manual script.
- SMS is console-only (development). No image upload. No online payments.

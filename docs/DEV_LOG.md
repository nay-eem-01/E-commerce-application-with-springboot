# Dev log

## Where we are

Phase 1 (backend rewrite) is done: nine-module modular monolith on Spring Boot 3.5 /
Java 21 / PostgreSQL, every storefront feature from the concept C design has an API, online
payments are built and switched off, order emails go through a transactional outbox. 136 unit
tests and an 84-check end-to-end script pass against a real database.

## Next up

Phase 2: the storefront in its own repository, following concept C exactly (`docs/ROADMAP.md`).

## Open items

- Rotate the JWT secret and MySQL password that were committed by the old code (`docs/SECURITY.md` S1).
- Get bKash and/or SSLCommerz sandbox credentials and run their flows before enabling (`docs/SECURITY.md` S7).
- Decide the pre-order deposit rule and the payment vendors (`docs/PLAN.md` §7 Q1, Q5).
- Pick an SMS provider and start sender-ID paperwork (`docs/PLAN.md` R1).
- Choose an email provider (SMTP account or SES/SendGrid) and a sending domain with SPF/DKIM.
- CI is not set up yet; run `./mvnw -B verify` before every push.

---

## 2026-11-02: Order emails (step 1.12)

**Done**
- New module `ecommerce-notification`: `OrderNotificationEvent` (placed, payment received,
  shipped, delivered, cancelled) from the order module becomes a rendered email in
  `notification_outbox` (V7) inside the same transaction.
- `OutboxDispatcher` claims due rows with `FOR UPDATE SKIP LOCKED`, sends outside any
  transaction, backs off 1 min / 5 min / 30 min / 2 h, and marks a row FAILED after 5 tries.
- Senders by property: `smtp` (Spring Mail, multipart text + HTML) and `log` (development).
  Optional staff copy of every new order (`MAIL_STAFF_ADDRESS`). Admin list and retry
  (`NOTIFICATION_MANAGE`).

**Decisions**
- Emails only when the shopper gave an email; no marketing, so no unsubscribe flow.
- Rendered at queue time (the email reflects the order as it was), deleted 30 days after sending.

**Bugs found and fixed before commit**
- `mail.smtp.auth` was hard-coded to true, so any relay without credentials failed every send.
  Found by running the app against a real SMTP server; now `MAIL_SMTP_AUTH` /
  `MAIL_SMTP_STARTTLS` (default true), with a regression test. The emails queued during the
  failure were delivered by the retry after the fix, as designed.

**Known limitations**
- No SPF/DKIM guidance per provider yet; SMS notifications still to do (roadmap 4.2).

## 2026-11-02: Online payments (step 1.11)

**Done**
- New module `ecommerce-payment` owns `payments` (moved from order) and `payment_events` (V6).
- `PaymentProvider` SPI with `bkash` (tokenized checkout: grant, create, execute, status,
  refund), `sslcommerz` (session, Order Validation API, transaction query, refund) and `fake`
  (development only) adapters. `PaymentRouting` maps each method to a provider from
  `app.payment.routes.*` and fails startup on a bad configuration.
- Checkout: COD unchanged; an online order reserves stock, commits, then opens the payment
  outside the transaction and returns `paymentAction.url`. Callbacks (`/api/payments/{provider}/callback`)
  and IPNs (`/ipn`) are verified with the vendor and redirect to `PAYMENT_RETURN_URL`.
- `POST /api/orders/pay` retries a payment; unpaid online orders expire after 30 minutes
  (order cancelled, stock restored); unpaid online orders cannot be packed; cancelling a paid
  online order refunds it; money arriving late is refunded automatically.

**Decisions**
- Production default stays `PAYMENT_ENABLED_METHODS=COD`; development enables BKASH and
  CARD on the fake provider so the storefront can be built against real redirects.
- Vendor credentials are environment-only; switching vendor is `PAYMENT_ROUTE_<METHOD>`.

**Bugs found and fixed before commit**
- V6 converted `VOIDED` to `CANCELLED` before dropping the old status CHECK, so it failed on
  any database that already had a cancelled COD payment. Caught by applying V1 to V5 with data,
  then V6; the check is now dropped first.

**Breaking API changes**
- `PaymentStatus.VOIDED` is now `CANCELLED`; new statuses `FAILED`, `EXPIRED`, `REFUNDED`.
- `paymentAction` is null for an online order whose provider was unreachable.

**Known limitations**
- Not run against vendor sandboxes yet (no credentials). bKash token cache and the expiry
  job are per instance. SSLCommerz IPN hash check not added (validation API is authoritative).

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

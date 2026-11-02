# Senpai Supply backend: plan and decisions

**Status:** Phase 1 (rewrite) done, see `docs/ROADMAP.md`
**Date:** 2026-11-02
**Companions:** `docs/ERD.md` (schema), `docs/API.md` (contract for the frontend),
`docs/SECURITY.md` (threat model and audit), `docs/GIT_WORKFLOW.md` (how we commit and push)

---

## 1. What we are building

The backend for an anime merchandise shop in Bangladesh: figures, cosplay costumes, water
bottles and bags. The storefront design (concept C, "night market") puts the product first:
a 360 viewer, price, stock and delivery cost above the fold, guest checkout with cash on
delivery, phone-code sign-in, order tracking.

> Shopper browses → product page (360, specs, story) → cart → checkout as guest or member →
> COD order → staff pack, ship, deliver → COD collected → verified-buyer review

Every design decision keeps four questions answerable: **what** was sold, **at which
price**, to **whom**, and **where is it now**.

---

## 2. Decisions taken (settled; raise it before reopening)

| # | Decision | Choice | Reasoning |
|---|---|---|---|
| 1 | Structure | **Multi-module Maven, modular monolith** (same as delivery_app) | One deployable, but the compiler stops `cart` reaching into `order`. Gives boundary enforcement, not runtime scaling. A module can later become a service without rewriting callers, because modules already talk through small interfaces and events (§4). |
| 2 | Module ownership | **Per domain; each module owns its tables** | No shared persistence module. Cross-module links are plain id columns in Java (no `@ManyToOne` across modules) but real foreign keys in PostgreSQL. |
| 3 | Web stack | **Servlet MVC + JPA** | Checkout is one transaction: stock + order + items + history + payment + listeners. Straightforward with JPA; painful in reactive. |
| 4 | Database | **PostgreSQL 15+** (was MySQL); tested on 16, PostgreSQL 17 (used by the other projects) works unchanged | `NULLS NOT DISTINCT` unique constraints (15+), partial indexes, `timestamptz`, CHECK constraints, triggers. Nothing version-specific beyond 15. Same engine as delivery_app and AgroTrends. |
| 5 | Schema | **Flyway + `ddl-auto=validate`** | `update` cannot express `NUMERIC(12,2)`, CHECKs or seed rows, and silently drifts between environments. |
| 6 | Shopper sign-in | **Phone + SMS code (OTP), no password** | Matches the design and the market: no password to forget, phone is needed for delivery anyway. First sign-in creates the account. Staff use email + password. |
| 7 | Tokens | **15-min access JWT bound to a server-side session + opaque single-use refresh token (30 days, rotated)** | Sign-out, bans and password changes take effect immediately. A reused refresh token revokes the whole session (theft detection). From AgroTrends. |
| 8 | Checkout | **Guest by default; account optional** | Forced sign-up is a top reason for checkout abandonment (Baymard). A signed-in order is linked to the account; guests track by order number + phone. |
| 9 | Prices | **Always computed on the server from the catalog** | Requests carry ids and quantities only. Orders snapshot names and prices so later edits never rewrite history. |
| 10 | Stock | **Atomic conditional UPDATE inside the order transaction** | `UPDATE ... SET stock = stock - q WHERE stock >= q` cannot oversell under concurrency. Rows are touched in product-id order to avoid deadlocks. |
| 11 | Duplicate orders | **`Idempotency-Key` header required on `POST /api/orders`** | A double tap or a retried request returns the same order instead of charging twice. |
| 12 | Payments | **`ecommerce-payment` module with a `PaymentProvider` SPI; the vendor per method is a property** | COD is roughly 75 to 95% of Bangladeshi e-commerce payments, so it stays the default. bKash (direct) and SSLCommerz (cards, internet banking, and bKash/Nagad as an aggregator) are built and switched off. `PAYMENT_ENABLED_METHODS` turns a method on; `PAYMENT_ROUTE_<METHOD>` picks its vendor. A new vendor is one class. Startup fails if an enabled method points at a missing, unsupported or unconfigured provider. |
| 13 | SMS | **`SmsSender` interface; console sender in development only** | The OTP flow works end to end today. A real provider (sender-ID approval takes days) is one class. The console sender refuses to run outside development. |
| 14 | Images | **URLs (CDN/object storage) validated as http(s)** | Upload with byte-sniffing (Tika) and S3 pre-signed URLs is a roadmap step, ported from delivery_app's plan. |
| 15 | Envelope | **`Response {status, message, payload, success, correlationId?}`** | Same envelope as delivery_app/AgroTrends. Lists return a stable `PageResponse` record instead of Spring's `PageImpl` JSON. |
| 16 | Admin bootstrap | **`INITIAL_ADMIN_EMAIL` env; password generated or from env; must change on first sign-in** | The old code seeded `admin/adminPass`. |
| 17 | Git | **Conventional commits, author nay-eem-01, no attribution, step branches** | From delivery_app/AgroTrends; see `docs/GIT_WORKFLOW.md`. |
| 18 | Online payment trust | **PAID only from the vendor's server answer, with matching amount, currency and reference** | Browser redirect parameters can be forged. Callbacks and IPNs trigger a server-to-server check (bKash execute/status, SSLCommerz validation/query). Money that arrives after an order was given up is refunded automatically. |
| 20 | Order emails | **`ecommerce-notification` with a transactional outbox; sender is a property (`MAIL_PROVIDER=smtp|log`)** | The email row is written in the same transaction as the order change, so no email for a rolled-back order and no SMTP call inside checkout. A dispatcher sends with `FOR UPDATE SKIP LOCKED` (safe on several instances), retries with backoff, and marks failures for an admin retry. Emails: placed (+ staff copy), payment received, shipped, delivered, cancelled. |
| 19 | Unpaid online orders | **Stock reserved at checkout; released when the payment expires (30 min) or the order is cancelled** | Prevents overselling while the shopper is on the payment page. Unpaid online orders cannot be packed. |

---

## 3. Starting point: audit of the old code

The repository held a tutorial-style single-module app (Spring Boot 3.3, MySQL). It was
replaced rather than patched; every finding below is fixed in the rewrite. Detail and the
threat model are in `docs/SECURITY.md`.

| # | Finding (old code) | Severity | Now |
|---|---|---|---|
| A1 | JWT signing secret hard-coded in `JwtUtils.key()` and in git history | Critical | `JWT_SECRET` env, ≥ 64 bytes, no default; app refuses to start without it. **Treat the old secret as public.** |
| A2 | `"/api/**"` permitAll: every endpoint, including admin product/category writes and all carts, was public | Critical | Deny by default; explicit public list; privileges on admin endpoints |
| A3 | Database password committed in `application.properties` | High | `.env` (git-ignored) / environment variables. **Rotate that password if it is used anywhere.** |
| A4 | Sign-up let the client choose `admin`/`seller` role | Critical | No sign-up role input; OTP sign-in creates CUSTOMER only |
| A5 | Seeded users `admin/adminPass`, `user1/password1` on every start | Critical | No default credentials (decision 16) |
| A6 | `GET /api/cart/all/carts` returned every user's cart to anyone | High | Removed; carts are scoped to the caller |
| A7 | `DELETE /api/cart/{cartId}/...` with no ownership check | High | Cart lines are found by id **and** user (404 otherwise) |
| A8 | Orders used any `addressId` (another user's address) | High | Address is entered at checkout and snapshotted; address book is scoped |
| A9 | Order total took the cart's total; each item's `orderedProductPrice` was set to the **whole cart total** | High (money) | Server-side pricing per line, `line_total = unit_price × qty` enforced by a DB CHECK |
| A10 | Prices as `double` | High (money) | `BigDecimal` / `NUMERIC(12,2)` with one rounding mode |
| A11 | No stock check; stock could go negative; race between orders | High | Atomic conditional decrement (decision 10) + `CHECK (stock >= 0)` |
| A12 | Payment status/gateway fields accepted from the client | High | Payment is created server-side; online methods must be confirmed server-to-server |
| A13 | Entities returned directly (lazy graphs, `@Data` on entities, eager collections) | Medium | Records as responses; `@Getter/@Setter` on entities; no cross-module relations |
| A14 | `ddl-auto=update` | Medium | Flyway + validate |
| A15 | Error responses leaked exception messages; several endpoints answered 302/FOUND | Medium | One handler, safe messages, correlation id; correct status codes |
| A16 | Cookie JWT without CSRF protection, sign-in errors returned 404 | Medium | Bearer header (no CSRF surface); uniform 401 message |
| A17 | Product images saved to local disk under the original file name | Medium | URLs only for now; upload with random names and byte-sniffing on the roadmap |
| A18 | No rate limits anywhere | Medium | Per-IP / per-phone / per-email limits on sign-in, OTP, orders and tracking |

---

## 4. Architecture

```
ecommerce-app (parent pom)
├── ecommerce-common   shared kernel: Response, PageResponse, AuditModel, ErrorCode, ResponseException,
│                      GlobalExceptionHandler, correlation id, LogMasks, Money, PhoneNumbers, Pages,
│                      CurrentUser/AuthenticatedUser, Privileges, RateLimiter, OrderPlacedEvent
├── ecommerce-user     OTP + password sign-in, sessions, refresh tokens, JWT filter, users, address book
├── ecommerce-catalog  categories, products, options, facts, box items, media (photos + 360 frames),
│                      search, inventory. Public surface: CatalogApi
├── ecommerce-cart     signed-in cart and wishlist (uses CatalogApi)
├── ecommerce-payment  payments, provider SPI (bkash, sslcommerz, fake), routing by property, callbacks/IPN,
│                      expiry job. Public surface: PaymentApi
├── ecommerce-notification  order emails: outbox, templates, dispatcher, sender chosen by property
├── ecommerce-order    delivery options, quote, checkout, orders, state machine, tracking (uses CatalogApi,
│                      PaymentApi; publishes OrderNotificationEvent). Public surface: PurchaseVerifier
├── ecommerce-review   verified-buyer reviews (uses CatalogApi, PurchaseVerifier)
└── ecommerce-api      bootstrap, security chain, config, Flyway migrations. Only deployable.
```

**Dependency direction:** `common ← user, catalog, payment, notification ← cart, order ← review ← api`.

**Boundary rules**
- A module never imports another module's entities or repositories.
- Cross-module reads go through a small public interface in an `api` package
  (`CatalogApi`, `PaymentApi`, `PurchaseVerifier`).
- Cross-module reactions go through events in common (`OrderPlacedEvent`): cart removes the
  bought lines and user saves the first address. Listeners run synchronously in the order's
  transaction, so a listener failure rolls the order back. Payment events
  (`PaymentSucceededEvent`, `PaymentExpiredEvent`) work the same way: an expired payment
  cancels its order and restocks it. `OrderNotificationEvent` carries an order snapshot to the
  notification module, which never reads order tables.
- The caller's identity reaches every module through `CurrentUser` / `AuthenticatedUser`
  (common), implemented by the user module's principal. No module depends on `user`.

### Scaling path: what changes, and what does not

| When | Change | Code touched |
|---|---|---|
| A second app instance | Rate limits move to Redis; the payment expiry job gets a lock (ShedLock); the bKash token cache becomes shared | One new `RateLimiter` implementation; one annotation; one cache |
| Auth lookups show up in profiles | Cache session + user lookups (short TTL, evicted on revoke) | `JwtAuthenticationFilter` only |
| Read traffic on the catalog | HTTP caching / Redis cache on `ProductQueryService`; read replica for `GET` | Catalog service only |
| Search needs ranking or typo tolerance | Generated `tsvector` column + GIN index, later a search engine | `ProductSpecifications.search` only |
| Order volume needs async work (SMS, analytics) | Same outbox pattern as email; later a broker reads the outbox | Event publisher; listeners stay |
| Email volume or deliverability | Swap SMTP for an API sender (SES, SendGrid) | One `EmailSender` class + `MAIL_PROVIDER` |
| A new payment vendor (Nagad direct, aamarPay, a bank) | One `PaymentProvider` implementation + `PAYMENT_ROUTE_<METHOD>` | That class only |
| A team owns one domain | Extract a module to a service; its `api` interface becomes an HTTP/gRPC client | That module + its client |

Nothing in this list requires changing controllers or the frontend contract.

---

## 5. Engineering rules (non-negotiable)

- **Two layers of authorization.** Privilege check at the controller for admin actions;
  ownership in the service. The acting user comes from the token, never from input.
- **Don't leak existence of private data.** Another shopper's order, address or cart line is
  404. Public content (reviews) is 403 when not yours.
- **Money:** `BigDecimal`, `NUMERIC(12,2)`, BDT, `Money` helper, one rounding mode. Never `double`.
- **Prices and totals are computed on the server.** Orders snapshot them.
- **Legal order transitions live in `OrderStateMachine` only.** Staff send an action, never a
  status; an illegal action is 409. `order_status_history` is append-only (DB trigger).
- **Every list endpoint is paginated** (max 100, sort allowlist).
- **Return DTOs (records), never entities.** Bean Validation on every request DTO.
- **Timestamps are UTC** (`Instant` / `timestamptz`).
- **Never log** tokens, passwords, OTP codes or full phone numbers (use `LogMasks`).

---

## 6. Out of scope for now (goes in the roadmap, not in code)

Real SMS provider and SMS order notifications · live payment credentials and sandbox certification · Nagad direct
integration (SSLCommerz covers Nagad today) · image upload · email receipts · promo codes ·
gacha mechanic · admin web UI · courier integration (e.g. a delivery_app client) ·
analytics · multi-currency · product variants with more than one option group.

---

## 7. Open questions

| # | Question | Needed by |
|---|---|---|
| 1 | Pre-order deposits: online payments currently charge the full order total; COD collects everything on delivery and `depositTotal` is informational. Should a pre-order charge only its deposit online? | Activating online payments |
| 5 | Payment vendors: direct bKash merchant account, SSLCommerz store, or both? (Both are built; routing decides.) | Activating online payments |
| 2 | Returns: who inspects, and is stock restored automatically for unopened returns? Today staff restock manually. | Returns flow |
| 3 | Brand name and SMS sender ID ("Senpai Supply" is a placeholder in SMS text). | SMS provider |
| 4 | Delivery partner: own riders or a courier (Steadfast/Pathao/delivery_app)? Affects SHIPPED → DELIVERED updates. | Courier integration |

---

## 8. Risks

- **R1, SMS provider lead time.** Sender-ID approval is on the provider's calendar. Start the
  paperwork early; the code does not wait for it (console sender in development).
- **R2, In-memory rate limits.** Correct for one instance only. Moving to Redis is a
  prerequisite for running two instances (§4).
- **R3, Old secrets in git history.** The old JWT secret and DB password remain readable in
  history. Rotation is what protects you; a history purge is optional and the owner's call.

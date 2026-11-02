# Roadmap

Phased steps; one small PR each (see `docs/GIT_WORKFLOW.md`). Tick a step in the same PR
that finishes it.

**Progress:** Phase 1 done (11/11). Phase 2: 0/7. Phase 3: 0/6. Phase 4: 0/6.

## Phase 1: rewrite the backend (done)

- [x] 1.1 Multi-module build, shared kernel (envelope, errors, pagination, money, phone, rate limiter)
- [x] 1.2 User: phone OTP sign-in, staff password sign-in, sessions, single-use refresh tokens, admin bootstrap
- [x] 1.3 User: profile, change password, address book, admin user list and ban
- [x] 1.4 Catalog: categories, products with one option group, facts, box items, photos and 360 frames
- [x] 1.5 Catalog: public list/filter/sort/search, product page, related; admin CRUD and stock; `CatalogApi`
- [x] 1.6 Cart and wishlist with guest-cart merge
- [x] 1.7 Order: delivery options, quote, guest/member checkout, idempotency, atomic stock, COD, tracking
- [x] 1.8 Order: state machine, append-only history, customer cancel, admin actions
- [x] 1.9 Reviews from verified buyers
- [x] 1.10 Flyway V1 to V5, dev demo catalog, security config, unit tests, end-to-end script, docs
- [x] 1.11 Payment module: provider SPI, bKash and SSLCommerz adapters (off by default), routing by property, verified callbacks/IPN, retry, expiry, refunds (V6)

## Phase 2: storefront (frontend repository, follows concept C exactly)

- [ ] 2.1 Repo scaffold following agrotrends_web conventions (Vite + React + TypeScript), API client with refresh handling, design tokens from the prototype
- [ ] 2.2 Home, category lists with filters, search
- [ ] 2.3 Product page: 360 viewer on `spinFrames`, facts, options, tabs, related, reviews
- [ ] 2.4 Guest cart (browser storage) + quote; member cart + merge
- [ ] 2.5 Checkout with Idempotency-Key, payment redirect and result page, retry payment, confirmation, guest tracking
- [ ] 2.6 OTP sign-in, account: orders, wishlist, addresses
- [ ] 2.7 Playwright end-to-end tests for browse → checkout → track

## Phase 3: production readiness

- [ ] 3.1 CI: `./mvnw verify`, dependency scanning, gitleaks
- [ ] 3.2 Testcontainers integration tests (migrations, checkout transaction, ownership) replacing the manual script in CI
- [ ] 3.3 Real SMS provider behind `SmsSender`
- [ ] 3.4 Image upload: byte-sniffing, random keys, S3 + CDN, admin upload endpoint
- [ ] 3.5 Dockerfile, Compose for staging, deploy checklist, backups
- [ ] 3.6 Redis-backed `RateLimiter` (prerequisite for a second instance)

## Phase 4: growth features

- [ ] 4.1 Activate online payments: sandbox-test bKash and SSLCommerz, set credentials, `PAYMENT_ENABLED_METHODS=COD,BKASH,CARD`; decide the pre-order deposit rule (PLAN §7 Q1)
- [ ] 4.2 Email/SMS order notifications via an outbox
- [ ] 4.3 Promo codes
- [ ] 4.4 Courier integration (delivery_app or Steadfast/Pathao) for SHIPPED/DELIVERED updates
- [ ] 4.5 Admin web UI
- [ ] 4.6 Gacha corner

# Senpai Supply: anime merchandise shop backend

Backend for an online shop selling anime figures, cosplay costumes, water bottles and bags
in Bangladesh. Guest checkout with cash on delivery, phone-code sign-in, product pages built
around a 360 viewer, order tracking and verified-buyer reviews.

**Stack:** Java 21 · Spring Boot 3.5 · PostgreSQL 15+ (16 and 17 fine) · Flyway · Spring Security + JWT ·
springdoc-openapi · Maven multi-module (modular monolith)

## Modules

| Module | What it does |
|---|---|
| `ecommerce-common` | Shared kernel: response envelope, errors, pagination, money, phone numbers, rate limiter, events |
| `ecommerce-user` | Phone OTP and password sign-in, sessions, refresh tokens, profile, address book, admin users |
| `ecommerce-catalog` | Categories, products, options, specs, photos and 360 frames, search, inventory |
| `ecommerce-cart` | Signed-in cart and wishlist, guest-cart merge |
| `ecommerce-payment` | Payments behind a provider interface: COD, bKash, SSLCommerz (cards, banks, wallets); vendor chosen by property |
| `ecommerce-order` | Delivery options, quote, checkout, orders, state machine, guest tracking |
| `ecommerce-review` | Reviews from buyers who received the product |
| `ecommerce-api` | Bootstrap, security, configuration, Flyway migrations (the deployable) |

## Run locally

1. PostgreSQL 15 or newer, with a database and user for the app.
2. Configure:
   ```bash
   cp .env.example .env
   ```
   Fill `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` (`openssl rand -base64 64`),
   `CORS_ALLOWED_ORIGINS`, and optionally `INITIAL_ADMIN_EMAIL`.
3. Build and test:
   ```bash
   ./mvnw -B verify
   ```
4. Start (from the repository root, so it reads `./.env`):
   ```bash
   java -jar ecommerce-api/target/ecommerce-api-0.0.1-SNAPSHOT.jar
   ```
   The development profile loads a demo catalog and prints sign-in codes to the log instead
   of sending SMS. Swagger UI: http://localhost:8080/swagger-ui.html

## Documentation

- [docs/API.md](docs/API.md): API contract for the storefront
- [docs/PLAN.md](docs/PLAN.md): decisions, architecture, scaling path, audit of the old code
- [docs/SECURITY.md](docs/SECURITY.md): threat model and controls
- [docs/ERD.md](docs/ERD.md): database schema
- [docs/GIT_WORKFLOW.md](docs/GIT_WORKFLOW.md): how to branch, commit and push
- [docs/ROADMAP.md](docs/ROADMAP.md) and [docs/DEV_LOG.md](docs/DEV_LOG.md): progress

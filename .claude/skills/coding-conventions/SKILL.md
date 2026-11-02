---
name: coding-conventions
description: Java/Spring coding conventions for the Senpai Supply e-commerce backend - module layout and boundaries, layering, Lombok rules, records for responses, money and phone handling, errors with ResponseException/ErrorCode, configuration and secrets, logging, Flyway, tests. Use whenever writing or changing Java code, SQL migrations or config in this repo.
---

# Coding conventions (Senpai Supply backend)

Ported from delivery_app and AgroTrends. Consistency beats local improvement: if the code
already does something one way, follow it.

## Stack

Java 21, Spring Boot 3.5, Maven wrapper (`./mvnw`), PostgreSQL 15+, Spring Data JPA, Flyway,
Spring Security 6 + JJWT 0.12, springdoc-openapi, Lombok. No ModelMapper: responses map
themselves with a static `from(...)`.

## Modules and boundaries

| Module | Holds |
|---|---|
| `ecommerce-common` | shared kernel only - envelope, errors, pagination, money, phone, `CurrentUser`, `Privileges`, `RateLimiter`, events. No business logic |
| `ecommerce-user` | auth, sessions, users, addresses |
| `ecommerce-catalog` | categories, products, inventory; public surface `catalog.api.CatalogApi` |
| `ecommerce-cart` | cart, wishlist |
| `ecommerce-payment` | payments, `spi.PaymentProvider` adapters (`provider/<vendor>`), routing by property; public surface `payment.api.PaymentApi` |
| `ecommerce-order` | checkout, orders, payments; public surface `order.api.PurchaseVerifier` |
| `ecommerce-review` | reviews |
| `ecommerce-api` | bootstrap, `SecurityConfig`, properties, Flyway migrations |

- A module never imports another module's `entity` or `repository` packages. Cross-module
  reads use the other module's `api` interface; reactions use an event in `common.event`.
- Across modules store the other side's **id** (`Long productId`), never a `@ManyToOne`.
- Inside a module: `controller` -> `service` -> `repository` -> `entity`. A controller never
  touches a repository. A service may call another service of the same module.
- Package layout per module: `controller`, `service`, `repository`, `entity`, `enums`,
  `dto/request`, `dto/response`, `config`, plus `api`, `listener`, `security`, `payment` where needed.

## Lombok and classes

- Entities and request DTOs: class-level `@Getter @Setter` (+ `@NoArgsConstructor` on entities).
  **Never `@Data` on an entity.**
- Constructor injection with `@RequiredArgsConstructor` on `private final` fields. No
  `@Autowired` fields. A hand-written constructor only when it does real work - say why.
- Response DTOs are `record`s with a static `from(...)`.
- Configuration: a validated `@ConfigurationProperties` record per group of keys.

## Entities and schema

- Entities extend `AuditModel` (id + audit columns) unless they are small child rows.
- Enums stored `@Enumerated(EnumType.STRING)`; long text `@Column(columnDefinition = "TEXT")`.
- Money: `BigDecimal` + `NUMERIC(12,2)`; compute through `common.money.Money`. Never `double`.
- Time: `Instant` / `timestamptz`, from an injected `Clock` when it matters to a test.
- Phones: normalize with `PhoneNumbers.normalize` to `+8801XXXXXXXXX`; show `toLocal`.
- **Schema changes only through a new Flyway file** `V<n>__<what>.sql` in `ecommerce-api`.
  Never edit an applied migration. Constraints that protect money or history go in SQL too.
- Dev-only data goes in `db/dev-seed` as an idempotent repeatable `R__` migration.

## Errors

- Throw `ResponseException` (helpers: `notFound`, `badRequest`, `conflict`, `forbidden`,
  `unauthorized`, `tooManyRequests`) with an `ErrorCode` constant. Add a constant for every
  new failure; never pass free text or `e.getMessage()`.
- Validation messages on request DTOs are shopper-facing sentences.
- Unexpected exceptions fall through to `GlobalExceptionHandler` (logged, generic 500 + correlation id).
- A read-only method other modules call and that throws expected `ResponseException`s must
  declare `noRollbackFor = ResponseException.class` (see `CatalogApiImpl.price`).

## Queries

- Every list: `Pages.of(pageNo, pageSize, sortBy, dir, ALLOWED_SORTS)` and return
  `PageResponse.of(page, X::from)`. Real database pagination only.
- Never bind a null string into a SQL function (`upper(:q)`): PostgreSQL types it as `bytea`.
  Use `''` for "no filter".
- Private lookups are scoped: `findByIdAndUserId`.

## Configuration and secrets

- Secrets only from environment variables (local: git-ignored `.env`). No default value for a
  secret anywhere; a missing secret fails startup.
- Non-secret tunables in `application.properties` with a safe default.

## Logging

- `@Slf4j`. INFO for business events (order placed, status changed), WARN for security events.
- Never log passwords, tokens, OTP codes, full phone numbers or emails (`LogMasks`), or request bodies.
  Request DTOs with secrets override `toString()`.

## Tests

- JUnit 5 + Mockito + AssertJ; unit tests need no Spring context and no database.
- Test what costs money: pricing and fees, stock, the order state machine, auth and token
  lifecycle, ownership (other user -> 404/403). Every bug fix ships with a regression test.
- End-to-end: `scripts/smoke_test.py` against a fresh development database (Testcontainers in CI is roadmap 3.2). Never H2.
- Run: `./mvnw -B verify`.

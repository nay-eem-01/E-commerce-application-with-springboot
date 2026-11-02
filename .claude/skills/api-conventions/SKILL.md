---
name: api-conventions
description: REST API conventions for the Senpai Supply backend - URL rules, the Response envelope, status codes, pagination, DTO and validation rules, money/time/phone formats, Idempotency-Key, OpenAPI annotations. Use whenever adding or changing a controller, request/response DTO or error, so the contract stays stable for the storefront.
---

# API conventions

The storefront builds against `docs/API.md`. Do not break a field name, path or status code
without recording it under "Breaking API changes" in `docs/DEV_LOG.md` in the same PR.

## URLs

- Base `/api`, plural nouns, kebab-case: `/api/products`, `/api/auth/refresh-token`.
- Public identity in the path is a slug or public number (`/api/products/{slug}`,
  `/api/orders/me/{orderNumber}`), not a database id, wherever the shopper sees it.
- "Mine" is `/me` (`/api/users/me`, `/api/orders/me`). The caller's id never appears in a URL, query or body.
- Admin endpoints live under `/api/admin/...`.
- Non-CRUD actions are POST sub-resources: `/api/orders/me/{n}/cancel`, `/api/orders/track`.
- State changes by staff send an **action** (`{action: "SHIP"}`), never a target status.

## Envelope

Every endpoint returns `ResponseEntity<Response>` built with `Response.ok/created/of`:

```json
{ "status": "OK", "message": "Order placed.", "payload": {}, "success": true }
```

- `message` is safe to show the shopper. Errors also carry `correlationId`.
- Lists: `payload` is `PageResponse {content, pageNo, pageSize, totalElements, totalPages, last}`.

## Status codes

| Code | Use |
|---|---|
| 200 / 201 | read or update / created |
| 400 | validation, malformed request, unknown sort field, missing Idempotency-Key, wrong OTP |
| 401 | no/invalid/expired/revoked token, wrong credentials (always the same message) |
| 403 | signed in but not allowed; must change password first; review before delivery |
| 404 | not found - **and private data that isn't yours** |
| 409 | out of stock, duplicate, illegal order transition, reused Idempotency-Key |
| 429 | rate limit |
| 500 | unexpected - generic message + correlation id |

## Pagination

`pageNo` (0-based), `pageSize` (default 20, clamped to 100), `sortBy` (per-endpoint
allowlist, default `creationDate`), `ascOrDesc` (`asc|desc`). Use `Pages.of`.

## DTOs and validation

- Request DTOs: `@Getter @Setter`, Bean Validation on every field, `@Valid` on every `@RequestBody`
  and nested list. Messages are full sentences the shopper can act on.
- **No caller identity and no prices in request DTOs.** Lists are size-capped.
- Response DTOs: records; never a password hash, token, or another user's contact data.
- Enums bind as the enum type.

## Formats

Money: number with two decimals, BDT. Times: ISO-8601 UTC (`Instant`). Months: `"2027-01"`.
Phones: accept any common form, return `01XXXXXXXXX`. Text content is plain text, never HTML.
`null` over omitted fields.

## Idempotency

Any POST that creates money-bearing state requires `Idempotency-Key` (16-64 chars). Same key
+ same owner returns the original result; a different owner gets 409.

## OpenAPI

- `@Tag` per controller, `@CommonApiResponses` on every controller.
- `@Operation(summary=...)` on every endpoint, written for someone who doesn't know the domain.
- `@SecurityRequirement(name = "jwtToken")` on every authenticated endpoint; its absence means
  the endpoint really is public - and it must then be in `SecurityConfig`'s public list.

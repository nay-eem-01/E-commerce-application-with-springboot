---
name: security-checklist
description: Security rules for the Senpai Supply backend - identity from the token, scoped queries, privileges, sessions and refresh tokens, OTP and sign-in limits, money and stock integrity, idempotency, secrets, logging, input/output rules. Use before writing or changing any endpoint, anything touching auth, tokens, prices, stock, orders, payments or user data.
---

# Security rules

Non-negotiable. If a rule blocks a requirement, raise it - do not work around it quietly.
The full threat model is `docs/SECURITY.md`; the old code's findings are `docs/PLAN.md` §3.

## Identity and authorization

1. The acting user comes from `CurrentUser` (the validated token), never from input. A DTO
   field naming the caller (`userId`) is a defect.
2. `SecurityConfig` denies by default. A new public endpoint must be added to its public list
   on purpose - and documented as public in `docs/API.md`.
3. Admin actions: `@PreAuthorize("hasAuthority('<PRIVILEGE>')")` with that resource's privilege
   from `common.security.Privileges`; a new privilege needs a Flyway insert granting it to ADMIN.
4. Private data (orders, addresses, cart lines): scoped query -> **404** when not yours.
   Public content (reviews): `CurrentUser.assertOwnerOrAdmin` -> **403**.
5. Roles are never chosen by the client.

## Tokens and sessions

- Access JWT 15 min, `typ=access`, `sid` = session public id; accepted only while the session
  is active and the user is ACTIVE. Refresh tokens: opaque, SHA-256 at rest, single use,
  rotated; reuse revokes the session.
- Sign-out revokes the session; password change and ban call `SessionService.revokeAll`.
- `JWT_SECRET` >= 64 bytes, no default. A bad token is a 401, never a 500.
- `mustChangePassword` accounts reach only `/api/users/me`, `/change-password`, `/api/auth/sign-out`.

## Sign-in

- OTP: CSPRNG 6 digits, BCrypt hash, 5 min, 5 tries, 60 s resend cooldown, limits per phone
  and per IP, one uniform error. Codes are never logged (only the dev console sender, which
  refuses to run outside development).
- Password sign-in: check credentials first, then status; same 401 for unknown email and
  wrong password; limits per email and IP counting unknown accounts.
- One password policy (`PasswordPolicy`), BCrypt only, never store/log/return a hash.

## Money, stock and orders

- Prices and totals come from the catalog on the server; request DTOs have no price fields.
- Stock changes only through `CatalogApi.reserve/release` inside the order transaction.
- `POST /api/orders` requires `Idempotency-Key`.
- Order status changes only through `OrderStateMachine`; history is append-only (DB trigger).
- A payment is marked PAID only by the server (COD on delivery, or a verified gateway callback).

## Input and output

- Product text is plain text; URLs must match `ProductRequest.URL_PATTERN` (http/https only).
- LIKE input is escaped; never concatenate SQL.
- Responses: records only; no hashes/tokens; guest tracking returns no address or phone.
- No exception detail, SQL or class names in responses; 500 = generic message + correlation id.

## Secrets and logging

- Environment variables / git-ignored `.env`; no defaults for secrets; never print `.env`.
- Never log tokens, passwords, OTP codes, full phones or emails (`LogMasks`), or request bodies.
- If a secret is ever committed: rotate first, then decide about a history purge.

## Per-endpoint checklist

- [ ] No caller identity in path/query/body; ownership asserted in the service
- [ ] Test for "other user -> 404/403" on anything user-owned
- [ ] `@SecurityRequirement` present, or the endpoint is deliberately in the public list
- [ ] Admin endpoint names its own privilege
- [ ] Rate limit if it sends SMS, checks credentials, creates orders or looks up by guessable input
- [ ] No secret, token or hash in a response, log line or test fixture
- [ ] Regression test for any bug fixed

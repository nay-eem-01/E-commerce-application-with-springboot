# Security analysis

**Date:** 2026-11-02 · **Scope:** the rewritten backend (all modules) · **Companion:** `.claude/skills/security-checklist`

This is the threat model, the controls that answer it, and what is still open. The audit of
the previous code is in `docs/PLAN.md` §3 (findings A1 to A18).

---

## 1. Assets and attackers

| Asset | Why it matters |
|---|---|
| Customer data (name, phone, address, order history) | Privacy; phone + address is enough to find someone |
| Money: prices, totals, stock, payment status | Underpricing, overselling, fake "paid" orders |
| Sessions and admin access | Admin can change prices, cancel orders, ban users |
| Availability of checkout | Order spam and SMS pumping cost money and stock |

| Attacker | Typical move |
|---|---|
| Anonymous internet user | Enumerate endpoints, call admin APIs, tamper with prices, spam orders/OTP |
| Signed-in customer | Read or change another customer's orders, addresses, cart, reviews (IDOR) |
| Someone holding a stolen token | Replay an access or refresh token |
| Someone with a DB dump | Read tokens, codes, passwords |

---

## 2. Controls by threat

### Authentication
| Threat | Control | Where |
|---|---|---|
| Forged or tampered JWT | HS512, secret ≥ 64 bytes from env, no default; startup fails on a weak secret; signature, expiry and `typ=access` checked | `JwtTokenProvider` |
| Stolen access token | 15-minute life; valid only while its server-side session is active; sign-out, ban and password change revoke sessions | `JwtAuthenticationFilter`, `SessionService` |
| Stolen refresh token | Opaque 256-bit, stored as SHA-256 only, single use; a reused token revokes the whole session | `SessionService.refresh` |
| OTP brute force | 6 random digits (CSPRNG), BCrypt-hashed, 5-minute life, 5 wrong tries kill the code, 60 s resend cooldown, per-phone and per-IP limits, uniform error | `OtpService`, `AuthService` |
| SMS pumping (cost attack) | 5 codes/hour per phone, 20/hour per IP | `AuthService.requestOtp` |
| Password guessing (staff) | BCrypt; 5 failures/15 min per email, 20/15 min per IP; same 401 for unknown email and wrong password; attempts counted for unknown accounts too | `AuthService.signIn` |
| Default credentials | None. First admin from `INITIAL_ADMIN_EMAIL`, generated password shown once, must change on first sign-in (other endpoints answer 403 until then) | `AdminBootstrap`, `PasswordChangeRequiredFilter` |
| Weak passwords | One policy (8 to 64, letter + digit + symbol, no spaces) | `PasswordPolicy` |

### Authorization
| Threat | Control |
|---|---|
| Unauthenticated access to private endpoints | Deny by default; explicit public list in `SecurityConfig` (GET catalog, quote, place order, track, sign-in) |
| Customer calling admin endpoints | `@PreAuthorize("hasAuthority('<PRIVILEGE>')")` naming that resource's privilege; privileges seeded by Flyway |
| IDOR on orders, addresses, cart lines | Scoped queries (`findByOrderNumberAndUserId`, `findByIdAndUserId`) → 404, existence not revealed |
| IDOR on reviews (public content) | Owner-or-admin in the service → 403 |
| Identity spoofing via request body | No request DTO carries a user id; identity comes from `CurrentUser` |
| Choosing your own role | No role input anywhere; OTP sign-in creates CUSTOMER only |

### Money and stock integrity
| Threat | Control |
|---|---|
| Client-supplied prices or totals | Request DTOs have no price fields; server prices every line from the catalog; DB CHECKs `total = subtotal + delivery_fee`, `line_total = unit_price × quantity` |
| Overselling under concurrency | Conditional `UPDATE ... WHERE stock >= q` inside the order transaction; `CHECK (stock >= 0)`; product-id lock order |
| Double orders (retries, double taps) | Required `Idempotency-Key`; same key returns the same order, a key reused by someone else is 409 |
| Fake "paid" status | A payment is PAID only from the vendor's server answer (bKash execute/status, SSLCommerz validation/query) whose amount, currency and reference match ours; callback/IPN parameters only identify the payment |
| Replayed or duplicated callbacks | Idempotent: a PAID payment ignores further callbacks; `@Version` on payments for a callback racing an IPN |
| A callback naming another vendor's payment | Payment found by reference/vendor id **and** the provider in the URL must match the payment's provider (404 otherwise) |
| Open redirect through the payment return | The shopper is always sent to the configured `PAYMENT_RETURN_URL`, never a URL from the request |
| Money arriving after cancellation/expiry | Detected on verification and refunded automatically |
| Vendor credentials | Environment only, no defaults; never logged; startup fails when an enabled method's vendor lacks them |
| The development fake provider in production | It reports itself unconfigured outside development/test, so routing refuses to start if a route points at it |
| Rewriting history | Orders snapshot prices and addresses; `order_status_history` UPDATE blocked by a DB trigger |
| Illegal status jumps | `OrderStateMachine`; staff send actions; illegal → 409 |

### Input, output and data exposure
| Threat | Control |
|---|---|
| Stored XSS through product content | Text fields are plain text (frontend renders text, never HTML); media URLs must be http(s) (blocks `javascript:` / `data:`) |
| SQL injection | JPA parameters / Criteria API everywhere; LIKE wildcards in search escaped |
| Over-exposure in responses | Records only; no password hash, token or other user's contact data; guest tracking returns no address/phone; stock exact counts shown only when low |
| Error detail leakage | One `GlobalExceptionHandler`; messages from `ErrorCode`; 500 returns a generic message + correlation id; whitelabel/stack traces off |
| Log injection / sensitive logs | Correlation id accepted only if it matches `[A-Za-z0-9-]{8,64}`; phones/emails masked; codes, tokens and passwords never logged (except the dev-only console SMS, which refuses to run outside development) |
| Unbounded queries | `pageSize` clamped to 100; `sortBy` allowlisted (400 otherwise); request lists capped (30 order lines, 50 cart lines) |

### Transport and platform
| Topic | Control |
|---|---|
| CORS | Allowlist from `CORS_ALLOWED_ORIGINS` (app will not start without it), no credentials |
| CSRF | Not applicable: bearer header, no cookies. Re-enable if cookie auth is ever added |
| Security headers | CSP `default-src 'none'; frame-ancestors 'none'`, `X-Frame-Options: DENY`, `X-Content-Type-Options`, HSTS on HTTPS |
| Client IP for limits | `request.getRemoteAddr()` only; behind a proxy set `server.forward-headers-strategy=native` (production default) so a client cannot spoof `X-Forwarded-For` |
| Actuator | Health and info only, no details |
| Swagger | Disabled in production |
| Secrets | Environment only; `.env` git-ignored; no defaults for secrets |

---

## 3. Open risks and next steps

| # | Risk | Plan |
|---|---|---|
| S1 | Old JWT secret and MySQL password are in git history | **Rotate** both if used anywhere. Purging history is optional (owner's call) |
| S2 | Rate limits are in memory (single instance) | Redis-backed `RateLimiter` before running two instances |
| S3 | Public tracking: order numbers have 40 random bits and need the matching phone; still rate limit at the reverse proxy too | Proxy rule on `/api/orders/track` at deploy |
| S4 | No dependency or secret scanning in CI yet | Add CI with `mvn verify`, OWASP dependency-check or Dependabot, gitleaks |
| S5 | Integration tests run against a local PostgreSQL by hand (`scripts/smoke_test.py`) | Testcontainers in CI (roadmap Phase 3) |
| S6 | Image upload not built | When built: Tika byte-sniffing, random object keys, size limits, private bucket + CDN |
| S7 | Online payments are built but untested against live vendor sandboxes (no credentials yet) | Run bKash and SSLCommerz sandbox flows before enabling; add SSLCommerz IPN hash (`verify_sign`) check as defence in depth; rate-limit callbacks at the proxy |
| S8 | Account deletion / data export not built | Needed for privacy requests; design with order snapshots (anonymize, keep financial record) |

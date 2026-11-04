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
| Malicious image uploads | Staff only (`PRODUCT_WRITE`), 60 a minute per user; at most 2 conversions at once per instance (a 40 MP decode is about 120 MB); 5 MB (multipart 6/7 MB, parsed lazily after security); type from the first bytes only; width, height and pixel count checked from the header before decoding; JPEG/PNG decoded only by the JDK's pure-Java readers and re-encoded, so only our own pixels reach the native WebP encoder; WebP never decoded on the server; random `<uuid>.webp` names; nothing from the upload (name, content type) is used |
| Products pointing at outside images | Saved media must be URLs this backend stored (`<UPLOADS_PUBLIC_BASE_URL>/<uuid>.webp`), except ones already on the product |
| Personal data in analytics | View and search counters store no user, IP or User-Agent; terms with 7+ digits (phone or order numbers) or an `@` (emails) are dropped; failed counter writes log only the exception type (PostgreSQL batch errors inline the values); insights responses are aggregates only, behind `INSIGHTS_READ` |
| Unbounded queries | `pageSize` clamped to 100; `sortBy` allowlisted (400 otherwise); request lists capped (30 order lines, 50 cart lines) |

### Email
| Threat | Control |
|---|---|
| HTML injection through names or product text | Every value is HTML-escaped in the HTML part; links are built only from `STOREFRONT_URL` and our generated order number |
| Email for an order that never committed, or duplicates | Outbox row written in the order's transaction; `dedup_key` unique |
| Personal data lingering | Outbox keeps the rendered email; sent rows are deleted after 30 days; the admin list never returns bodies |
| Address leaks in logs | Recipients masked (`LogMasks`); bodies logged only at DEBUG by the development log sender |
| SMTP credentials | Environment only; `MAIL_PROVIDER=smtp` without a host fails startup; production warns when emails are only logged |

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
| S7 | Online payments are built but untested against live vendor sandboxes (no credentials yet) | Run bKash and SSLCommerz sandbox flows before enabling; add SSLCommerz IPN hash (`verify_sign`) check as defence in depth; rate-limit callbacks at the proxy |
| S9 | View and search counters are fed by public GETs, so a script can inflate them. Memory and rows are bounded (views by products; at most 20 000 new search terms per day per instance, ≤ 60 chars, kept 400 days; crawler User-Agents skipped) but the numbers can be skewed | Per-IP rate limit on `/api/products` at the proxy; treat insights as approximate |
| S10 | WebP uploads are stored as sent (never decoded on the server, by design): an animated WebP passes if its canvas is within the limits, and its frames are decoded by shoppers' browsers. Staff only, 5 MB | Re-encode WebP once a decoder runs in a sandbox, or reject the `ANIM` flag if it is ever abused |
| S11 | The in-memory upload limit (60 a minute) and the conversion permits (2) are per instance | Same as S2: a shared store before a second instance |
| S8 | Account deletion / data export not built | Needed for privacy requests; design with order snapshots (anonymize, keep financial record) |

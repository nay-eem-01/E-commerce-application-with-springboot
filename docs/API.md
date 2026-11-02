# API contract (for the frontend)

Swagger UI (development): `http://localhost:8080/swagger-ui.html`. This page is the summary;
Swagger has every field. Breaking changes are recorded in `docs/DEV_LOG.md`.

## Conventions

- Base path `/api`, JSON, UTF-8. Money is BDT as a number with two decimals (`1450.00`).
  Times are ISO-8601 UTC (`2026-11-02T10:00:00Z`). Enums are uppercase strings.
- Every response uses one envelope:
  ```json
  { "status": "OK", "message": "Order placed.", "payload": { }, "success": true }
  ```
  On failure `success` is false, `payload` is usually null, and `correlationId` is set. Show
  `message` to the shopper as is; quote `correlationId` in bug reports.
- Lists return `payload` as `{ content, pageNo, pageSize, totalElements, totalPages, last }`.
  Params: `pageNo` (0-based), `pageSize` (default 20, max 100), `sortBy`, `ascOrDesc` (`asc|desc`).
  An unknown `sortBy` is 400.
- Auth: `Authorization: Bearer <accessToken>`. Access tokens last 15 minutes. On 401, call
  `/api/auth/refresh-token` once with the refresh token; if that is 401 too, sign the user out.
  **Each refresh token works once**: store the new one every time. Two tabs refreshing with the
  same token will sign the user out (by design: it looks like theft), so refresh from one place.
- Phones: send what the shopper typed (`01712345678`, `+8801712345678`); responses use `01712345678`.

| Status | Meaning for the UI |
|---|---|
| 400 | Show `message` next to the form |
| 401 | Not signed in / session ended → refresh once, else go to sign-in |
| 403 | Signed in but not allowed (or admin must change password first) |
| 404 | Not found, or private and not yours |
| 409 | Conflict: out of stock, duplicate, illegal order step |
| 429 | Too many attempts: show `message`, disable the button for a minute |

## Endpoints

### Auth (`/api/auth`, public)
| Method | Path | Body → payload |
|---|---|---|
| POST | `/otp/request` | `{phone}` → `{expiresInSeconds, resendAfterSeconds}` |
| POST | `/otp/verify` | `{phone, code, name?}` → `TokenResponse {accessToken, refreshToken, tokenType, expiresIn, user}` (creates the account on first sign-in) |
| POST | `/sign-in` | `{email, password}` → `TokenResponse` (staff) |
| POST | `/refresh-token` | `{refreshToken}` → `TokenResponse` |
| POST | `/sign-out` | (auth) → null |

### My account (`/api/users/me`, auth)
| Method | Path | |
|---|---|---|
| GET / PUT | `/api/users/me` | profile `{id, name, phone, email, roles, hasPassword, mustChangePassword}`; PUT `{name, email?}` |
| POST | `/api/users/me/change-password` | `{currentPassword, newPassword}`; signs out every device |
| GET / POST | `/api/users/me/addresses` | list / create `{fullName, phone, district, area, addressLine, defaultAddress}` |
| PUT / DELETE | `/api/users/me/addresses/{id}` | edit / remove |

### Storefront (public)
| Method | Path | |
|---|---|---|
| GET | `/api/categories` | `[{id, slug, name, label, blurb, productCount}]` |
| GET | `/api/products` | filters `category, q, inStock, preorder, minPrice, maxPrice`; `sortBy` = `creationDate|price|name` → page of `ProductSummary` |
| GET | `/api/products/{slug}` | `ProductDetail` (below) |
| GET | `/api/products/{slug}/related` | up to 4 `ProductSummary` |
| GET | `/api/products/{slug}/reviews` | `{summary: {average, count, distribution}, reviews: page}` |

`ProductSummary`: `id, slug, name, categorySlug, specLine, price, compareAtPrice, stockState
(IN_STOCK|LOW_STOCK|SOLD_OUT|PREORDER), stockLeft (only when LOW_STOCK), preorderRelease
("2027-01"), deposit, hasOptions, thumbnailUrl`.

`ProductDetail` adds `maker, category{slug,name}, summary, description, story, storyQuote,
preorder, sized (show size guide), optionName, options[{id, value, priceDelta, price,
stockState, stockLeft, swatch}], facts[{label, value}] (first four = the fact grid),
boxItems[], photos[{url, altText}], spinFrames[url] (360 viewer, in order), storyImage`.
Text fields are **plain text** with paragraphs split by a blank line. Render as text, never HTML.

### Cart and wishlist (auth)
Guests keep the cart in the browser. After sign-in, send it once to `/api/cart/merge`.
| Method | Path | |
|---|---|---|
| GET | `/api/cart` | `{items[...], itemCount, subtotal}`, priced live; `available=false` lines are archived products |
| PUT | `/api/cart/items` | `{productId, optionId?, quantity}` (0 removes, max 10) |
| DELETE | `/api/cart/items/{itemId}` | |
| POST | `/api/cart/merge` | `{items: [{productId, optionId?, quantity}]}` |
| DELETE | `/api/cart` | empty the cart |
| GET | `/api/wishlist` · `/api/wishlist/ids` | product summaries · ids (for hearts) |
| PUT / DELETE | `/api/wishlist/{productId}` | save / remove |

### Checkout and orders
| Method | Path | Auth | |
|---|---|---|---|
| GET | `/api/checkout/delivery-options` | public | districts, fees, free threshold, estimates, enabled `paymentMethods` |
| POST | `/api/checkout/quote` | public | `{items, district?}` → lines, subtotal, deliveryFee, total, depositTotal, amountToFreeDelivery, allInStock |
| POST | `/api/orders` | optional | header **`Idempotency-Key`** (new UUID per checkout attempt; reuse it on retry). Body `{items, fullName, phone, email?, district, area, addressLine, note?, paymentMethod, saveAddress}` → `OrderResponse` (201) |
| POST | `/api/orders/track` | public | `{orderNumber, phone}` → status, items, total, timeline (no address) |
| POST | `/api/orders/pay` | public | `{orderNumber, phone}` → `PaymentAction` for an unpaid online order (retry) |
| GET | `/api/orders/me` | auth | my orders (page) |
| GET | `/api/orders/me/{orderNumber}` | auth | full order + timeline |
| POST | `/api/orders/me/{orderNumber}/cancel` | auth | only while `PLACED` |

Order statuses: `PLACED → PACKED → SHIPPED → DELIVERED`, or `CANCELLED`, `RETURNED`.
If the order has an `email`, the shopper is emailed when it is placed, when an online payment
is received, and when it is shipped, delivered or cancelled. Email links point to
`STOREFRONT_URL/track?order=...` and `STOREFRONT_URL/orders/{orderNumber}`; the storefront
should have both routes.
`OrderResponse.amountDueOnDelivery` is what to tell a COD shopper to have ready.
`paymentStatus`: `PENDING, PAID, FAILED, CANCELLED, EXPIRED, REFUNDED`.
`paymentAction`: `{type: "NONE"}` for COD; `{type: "REDIRECT", url}` for an online method
(send the browser to `url`); `null` if the provider was unreachable (show "Try paying again").

### Payment callbacks (called by providers, not by the storefront)
| Method | Path | |
|---|---|---|
| GET/POST | `/api/payments/{provider}/callback` | Browser return. Verified with the provider, then **303** to `PAYMENT_RETURN_URL?order=SS-...&payment=PAID|FAILED|CANCELLED|PENDING|UNKNOWN` |
| POST | `/api/payments/{provider}/ipn` | Server-to-server notification, verified the same way |

`provider` is `bkash`, `sslcommerz` or (development) `fake`.

### Admin (auth + privilege)
| Method | Path | Privilege |
|---|---|---|
| GET/POST | `/api/admin/categories`, PUT/DELETE `/{id}` | `CATEGORY_WRITE` |
| GET | `/api/admin/products`, `/api/admin/products/{id}` | `PRODUCT_READ_ALL` |
| POST, PUT `/{id}`, PATCH `/{id}/stock`, DELETE `/{id}` (archive) | `/api/admin/products` | `PRODUCT_WRITE` |
| GET | `/api/admin/orders`, `/api/admin/orders/{orderNumber}` | `ORDER_READ_ALL` |
| PATCH | `/api/admin/orders/{orderNumber}/status` `{action: PACK|SHIP|DELIVER|CANCEL|RETURN, note?}` | `ORDER_UPDATE` |
| GET | `/api/admin/users?q=` | `USER_READ` |
| PUT | `/api/admin/users/{id}/status` `{status: ACTIVE|BANNED}` | `USER_UPDATE` |
| PATCH | `/api/admin/reviews/{id}/status` `{status: PUBLISHED|HIDDEN}` | `REVIEW_MODERATE` |
| GET | `/api/admin/notifications?status=PENDING|SENT|FAILED&orderNumber=` | `NOTIFICATION_MANAGE` |
| POST | `/api/admin/notifications/{id}/retry` (FAILED only) | `NOTIFICATION_MANAGE` |

## Flows the storefront needs

**Product page:** `GET /api/products/{slug}` + `/related` + `/reviews` in parallel. The 360
viewer uses `spinFrames`; when it is empty, show the first photo.

**Cart page (guest or member):** `GET /api/checkout/delivery-options` once (cache it), then
`POST /api/checkout/quote` with the cart lines whenever the cart changes. Never compute the
total in the browser.

**Checkout:** generate `Idempotency-Key = crypto.randomUUID()` when the checkout page opens.
`POST /api/orders` with it; on a network error retry with the **same** key. On 409
`OUT_OF_STOCK`, re-quote and show which lines changed (`payload.productIds`).
Offer only `deliveryOptions.paymentMethods`. For an online method, redirect to
`paymentAction.url`; the shopper comes back to the result page with `order` and `payment` in
the query. The result page then loads the order (members: `/api/orders/me/{n}`; guests:
`/api/orders/track`) because the query string is only a hint. On `FAILED`/`CANCELLED`, offer
`POST /api/orders/pay`. Unpaid online orders are cancelled after 30 minutes.

**Local development:** online methods use the `fake` provider: its "payment page" is the
callback URL with `outcome=success`; change it to `fail` or `cancel` to test those paths.

**Sign-in:** `/otp/request` → show the code input → `/otp/verify` → store tokens →
`POST /api/cart/merge` with the browser cart → clear the browser cart.

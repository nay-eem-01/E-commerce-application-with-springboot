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
| GET | `/api/categories` | `[{id, slug, name, label, blurb, productCount, comingSoon}]` (a coming-soon category has no products yet; offer early access) |
| GET | `/api/characters` | `[{id, slug, name, label, blurb, productCount}]`: the shop's own characters (collections) |
| GET | `/api/products` | filters `category, character, set (true: sets only), q, tag (repeatable, "Capacity:750 ml"; values of one group OR, groups AND), inStock, preorder, minPrice, maxPrice`; `sortBy` = `creationDate|price|name` → page of `ProductSummary` |
| GET | `/api/products/facets` | `?category=&character=` → `{groups: [{name, values: [{value, count}]}]}` for the filter sheet (measured values sorted by size) |
| POST | `/api/early-access` | `{categorySlug, phone?, email?}` (one needed) → 200 "You are on the list"; same answer when already listed; 404 unless the category is coming soon; rate limited |
| GET | `/api/products/{slug}` | `ProductDetail` (below) |
| GET | `/api/products/{slug}/related` | up to 4 `ProductSummary` |
| GET | `/api/products/{slug}/reviews` | `{summary: {average, count, distribution}, reviews: page}` |

`ProductSummary`: `id, slug, name, categorySlug, specLine, price, compareAtPrice, stockState
(IN_STOCK|LOW_STOCK|SOLD_OUT|PREORDER), stockLeft (only when LOW_STOCK), preorderRelease
("2027-01"), deposit, hasOptions, thumbnailUrl, characterSlug, newArrival (listed in the last 30 days),
swatches[{optionId, value, swatch, stockState}] (colour options: card dots and quick add), set (a set of other products)`.

`ProductDetail` adds `maker, category{slug,name}, summary, description, story, storyQuote,
preorder, sized (show size guide), optionName, options[{id, value, priceDelta, price,
stockState, stockLeft, swatch}], facts[{label, value}] (first four = the fact grid),
boxItems[], photos[{url, altText}], spinFrames[url] (360 viewer, in order), storyImage, character{slug,name,label},
tags[{group, value}], newArrival, bundleItems[ProductSummary] (for a set: what it holds),
sets[{set: ProductSummary, saving}] (sets containing this product: "Complete the set")`. A set has its own
price and stock; buying it does not take stock from the products it holds.
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
| GET | `/api/admin/products`, `/api/admin/products/{id}`, `/api/admin/products/{id}/stock` (exact `{productId, stock, options: [{id, value, stock}]}`), `/api/admin/products/{id}/edit` (the product in the exact shape `PUT` takes: `categoryId`, `characterId`, exact option stock, `media: [{kind, url, altText}]` in order, `bundleProductIds`, `tags`) | `PRODUCT_READ_ALL` |
| POST | `/api/admin/uploads` (multipart `file`: JPEG, PNG or WebP, at most 5 MB) → 201 `{url}` of a stored WebP; 60 a minute per user | `PRODUCT_WRITE` |
| POST, PUT `/{id}`, PATCH `/{id}/stock`, DELETE `/{id}` (archive) | `/api/admin/products` | `PRODUCT_WRITE` |
| GET | `/api/admin/orders`, `/api/admin/orders/{orderNumber}` | `ORDER_READ_ALL` |
| PATCH | `/api/admin/orders/{orderNumber}/status` `{action: PACK|SHIP|DELIVER|CANCEL|RETURN, note?}` | `ORDER_UPDATE` |
| GET | `/api/admin/users?q=` | `USER_READ` |
| PUT | `/api/admin/users/{id}/status` `{status: ACTIVE|BANNED}` | `USER_UPDATE` |
| GET | `/api/admin/reviews?status=PUBLISHED|HIDDEN&productId=` newest first: `{id, productId, productName, authorName, rating, body, status, createdAt}` (display name only, no contact details) | `REVIEW_MODERATE` |
| PATCH | `/api/admin/reviews/{id}/status` `{status: PUBLISHED|HIDDEN}` | `REVIEW_MODERATE` |
| GET/POST | `/api/admin/characters`, PUT/DELETE `/{id}` | `CATEGORY_WRITE` |
| GET | `/api/admin/early-access?category=` | `USER_READ` |
| GET | `/api/admin/notifications?status=PENDING|SENT|FAILED&orderNumber=` | `NOTIFICATION_MANAGE` |
| POST | `/api/admin/notifications/{id}/retry` (FAILED only) | `NOTIFICATION_MANAGE` |
| GET | `/api/admin/insights/overview`, `/products?sort=&dir=`, `/signals`, `/breakdown?by=`, `/searches` (all take `from`, `to`) | `INSIGHTS_READ` |

### Product images

Upload first, then put the returned URL in the product's `media`. JPEG and PNG are converted to WebP (longest side at
most 2 400 px, never scaled up); WebP is stored as sent. Rejected with 400: empty, over 5 MB, another format (checked
from the bytes, not the name), over 8 000 px a side or 40 megapixels, unreadable (CMYK JPEG, truncated). A product's
media must be URLs uploaded here, except URLs already on that product (400 otherwise). In development images are
stored on disk and served at `/uploads/<uuid>.webp`; in production from S3 (or R2 / MinIO) under
`UPLOADS_PUBLIC_BASE_URL`, cached for a year (names are never reused).

### Admin insights (`/api/admin/insights`, `INSIGHTS_READ`)

Demand signals for stock and marketing decisions. Every endpoint takes `from` and `to` (ISO dates, Dhaka days, both
inclusive; default the last 30 days ending today; at most 366 days; `from > to` is 400). Aggregates only: no phone,
email or other personal data.

- A **counted order** was placed in the period and is not `CANCELLED` or `RETURNED`. **Revenue** is its subtotal
  (no delivery fee). A buyer (by order phone) is **new** when their first counted order ever is in the period.
- **Views** are successful `GET /api/products/{slug}`; **searches** are `GET /api/products?q=` on page 0. Requests
  without a User-Agent or from crawlers are not counted. Counts reach the database within a minute and are approximate.
- **Demand** (`wishlists`, `cartUnits`, `cartMembers`) is today's snapshot of signed-in members; guest carts are not seen.

| GET | Payload |
|---|---|
| `/overview` | `{from, to, current, previous, daily: [{day, orders, revenue}]}`; totals are `{orders, units, revenue, averageOrder\|null, deliveryFees, cancelled, cancelRate (0..1), returned, newBuyers, returningBuyers, views, searches, zeroResultSearches}`; `previous` is the same length just before; `daily` has every day |
| `/products?sort=revenue\|units\|views\|conversion\|wishlists\|daysLeft\|stock&dir=desc\|asc` | every active product: `{id, slug, name, category, character, preorder, stock, views, orders, units, revenue, conversion (orders/views; null without views or when orders outnumber views)\|null, wishlists, cartUnits, cartMembers, daysLeft\|null, signals: [type]}`; nulls sort last |
| `/signals` | up to 30 `{type, title, reason, productId, productSlug, term, categorySlug, value}` ranked by type, then by `value` |
| `/breakdown?by=CATEGORY\|CHARACTER\|DISTRICT\|PAYMENT_METHOD` | `[{name, orders, units, revenue, share (0..1)}]` by revenue; for category and character an order with two products of the group counts twice in `orders` |
| `/searches` | `{top: [{term, searches, zeroResults}], zeroResults: [...]}`, 20 each |

Signal types (thresholds in `app.insights.*`): `RESTOCK_SOON` (≤ 14 days of stock left), `WANTED_SOLD_OUT` (sold out,
≥ 3 saved or in carts), `LOW_STOCK_HIGH_DEMAND` (saved or in carts ≥ stock), `LOOKED_NOT_BOUGHT` (≥ 50 views, < 1 %
converting), `SLOW_MOVER` (in stock, none sold, older than the period), `SEARCH_GAP` (a term found nothing ≥ 3 times),
`COMING_SOON_DEMAND` (early-access signups). Pre-orders skip the restock, sold-out and slow-mover signals.

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

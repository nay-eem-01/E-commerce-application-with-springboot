# ERD

**Source of truth:** `ecommerce-api/src/main/resources/db/migration/V1..V10`. Hibernate only
validates against it (`ddl-auto=validate`). Every table except the small child/seed tables
carries the audit columns `created_by, last_modified_by, creation_date, last_modified_date`.

```mermaid
erDiagram
    USERS ||--o{ USERS_ROLES : has
    ROLES ||--o{ USERS_ROLES : grants
    ROLES ||--o{ ROLES_PRIVILEGES : has
    PRIVILEGES ||--o{ ROLES_PRIVILEGES : grants
    USERS ||--o{ USER_SESSIONS : "signed-in devices"
    USER_SESSIONS ||--o{ REFRESH_TOKENS : "rotated, single use"
    USERS ||--o{ ADDRESSES : "address book"
    OTP_VERIFICATIONS }o..|| USERS : "by phone, before the account exists"

    CATEGORIES ||--o{ PRODUCTS : contains
    PRODUCTS ||--o{ PRODUCT_OPTIONS : "one option group (size/colour/edition)"
    PRODUCTS ||--o{ PRODUCT_FACTS : specs
    PRODUCTS ||--o{ PRODUCT_BOX_ITEMS : "in the box"
    PRODUCTS ||--o{ PRODUCT_MEDIA : "photos, 360 frames, story image"

    USERS ||--o{ CART_ITEMS : owns
    PRODUCTS ||--o{ CART_ITEMS : "id only"
    USERS ||--o{ WISHLIST_ITEMS : saves

    USERS |o--o{ ORDERS : "null for guests"
    ORDERS ||--o{ ORDER_ITEMS : "price snapshot"
    ORDERS ||--o{ ORDER_STATUS_HISTORY : "append-only"
    ORDERS ||--o{ PAYMENTS : "attempts (order_id only)"
    PAYMENTS ||--o{ PAYMENT_EVENTS : "append-only audit"
    PRODUCTS ||--o{ ORDER_ITEMS : "id only"

    NOTIFICATION_OUTBOX }o..|| ORDERS : "dedup_key = order number : type (no FK)"

    PRODUCTS ||--o{ REVIEWS : about
    USERS ||--o{ REVIEWS : "verified buyer, one each"
```

## Module ownership (a table is written by exactly one module)

| Module | Tables |
|---|---|
| `ecommerce-user` | `users`, `roles`, `privileges`, `users_roles`, `roles_privileges`, `user_sessions`, `refresh_tokens`, `otp_verifications`, `addresses` |
| `ecommerce-catalog` | `categories`, `products`, `product_options`, `product_facts`, `product_box_items`, `product_media`, `characters` and `product_tags` (V8), `product_bundle_items` and `early_access_signups` (V9), `product_view_daily` and `search_term_daily` (V10) |
| `ecommerce-cart` | `cart_items`, `wishlist_items` |
| `ecommerce-payment` | `payments`, `payment_events` (taken over from order in V6) |
| `ecommerce-notification` | `notification_outbox` (V7) |
| `ecommerce-order` | `orders`, `order_items`, `order_status_history` |
| `ecommerce-review` | `reviews` |
| `ecommerce-insights` | none: reads `CatalogReport`, `DemandReport` and `SalesReport` |

Cross-module links (`cart_items.product_id`, `order_items.product_id`, `orders.user_id`,
`reviews.product_id`) are plain id fields in Java but real foreign keys in PostgreSQL.

## Rules the schema enforces

| Rule | How |
|---|---|
| Every account can sign in | `users_has_sign_in`: phone, or email + password hash |
| Emails are compared case-insensitively | `users_email_lowercase` + code lowercases |
| One default address per user | Partial unique index `addresses_one_default` |
| No negative stock | `CHECK (stock >= 0)` on products and options |
| A pre-order has a release month and deposit | `products_preorder_fields` |
| One cart line per product + option | `UNIQUE NULLS NOT DISTINCT (user_id, product_id, option_id)` (PostgreSQL 15+) |
| Totals add up | `orders_total_adds_up`, `line_total = unit_price * quantity` |
| A retried checkout cannot create a second order | `orders.idempotency_key UNIQUE` |
| Status history and payment events are never edited | `BEFORE UPDATE` triggers raise |
| One row per vendor payment | Partial unique index on `(provider, provider_payment_id)`; `payments.reference` unique |
| An email is queued once per order event | `notification_outbox.dedup_key UNIQUE` |
| One review per buyer per product | `reviews_one_per_buyer` |
| Removing a product option never breaks history | `order_items.option_id ON DELETE SET NULL` (label is snapshotted); cart lines cascade |

## Stock rule

A product without options sells from `products.stock`. A product with options sells from
each `product_options.stock`, and `products.stock` holds their sum so lists can filter and
sort on one column (`Product.recomputeStock()`; reservations decrement both atomically).

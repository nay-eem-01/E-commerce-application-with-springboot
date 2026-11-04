# Admin insights dashboard: design

Date: 2026-11-04. Covers the backend (this repository) and the admin screens in `ecommerce_web`.

## Intent

Nayeem wants an admin dashboard that gives **leads**, meaning demand signals for stock and marketing
decisions: what to restock, what to stock next, what to promote, and where buyers are. A dashboard with
CRUD only is not enough. It is not a contact list. Nobody gets called from this screen.

## Goals

- Sales over a period with a comparison to the previous period: revenue, orders, units, average order,
  cancellation rate, new vs returning buyers, a daily series.
- A product table that puts sales, views, wishlist/cart demand and stock side by side, with days of stock left.
- A **signals** list: short, ranked "do this" items computed from that data (restock soon, wanted but sold out,
  looked at but not bought, slow mover, searches that found nothing, coming-soon category demand).
- Breakdowns by category, character, district and payment method.
- Search terms: the most searched, and the ones that found nothing.
- Admin screens: the dashboard, orders (list, detail, status actions) and quick stock edits.

## Non-goals

- Phone or email lists, exports of people, CRM. CSV export is later.
- Browser-side tracking, guest-cart tracking, a step-by-step funnel (option 3, rejected for now).
- Full product, category, character, user and review editors in the web app (their APIs exist; later step).
- Real-time numbers. Data that is up to a minute old is fine.

## Anti-goals (a failure even if it works)

- Dashboard totals that disagree with the order list for the same period.
- The storefront gets slower: no database write on a shopper's request path.
- Personal data in the analytics tables or the insights responses.

## Approaches

| | Approach | Trade-off | Decision |
|---|---|---|---|
| Data | 1. Existing data only | No new tables, but blind to demand for things not stocked | |
| | **2. Existing data + server-side counters for searches and product views** | One small table pair, no frontend change; bot traffic makes views approximate | **Picked** |
| | 3. Browser click tracking | Richest, but a public write endpoint and a frontend tracker | Rejected for now |
| Build | **A. Each module exposes a read-only reporting interface; a new `ecommerce-insights` module combines them** | ~3 small interfaces more code; keeps the module rule | **Picked** |
| | B. Insights module runs SQL over every module's tables | Less code; breaks the `CLAUDE.md` boundary rule, and reports break silently on schema change | Rejected |
| UI scope | **Dashboard + orders + stock** | Daily-use screens first | **Picked** |

## Definitions (shared by every number)

- A period is `from`/`to` as local dates in **Asia/Dhaka**, both inclusive. Default: the last 30 days up to today.
  At most 366 days. "Previous period" is the same number of days immediately before.
- An order counts toward sales when `placed_at` falls in the period and its status is not `CANCELLED` or `RETURNED`.
  Cancelled and returned orders are counted separately. *(Builder's call: unpaid online orders still `PLACED`
  count until they expire; the order list shows them the same way.)*
- Revenue is the sum of `subtotal` (merchandise, no delivery fee). Delivery fees are reported apart.
- A buyer is identified by the order's phone number. A buyer is **new** in the period if their first ever
  counted order falls in it, otherwise **returning**. Only the counts leave the order module.
- Days of stock left = `stock / (units sold in the period / days in the period)`; null when nothing sold.
- Views and searches are the counters below; demand (wishlist count, units in members' carts) is today's snapshot.

## Backend

### New counters (catalog module, Flyway V10)

```sql
product_view_daily (day DATE, product_id BIGINT REFERENCES products ON DELETE CASCADE, views INT, PRIMARY KEY (day, product_id))
search_term_daily  (day DATE, term VARCHAR(60), searches INT, zero_results INT, PRIMARY KEY (day, term))
```

- `CatalogTraffic` (catalog `service`) keeps counts in memory (`ConcurrentHashMap` + `LongAdder`), flushed
  every 60 s by `@Scheduled` and on shutdown (`@PreDestroy`) with `INSERT ... ON CONFLICT DO UPDATE`. A crash loses
  at most a minute. Several instances add into the same rows, so it scales out as is.
- A **view** is a successful `GET /api/products/{slug}`. A **search** is `GET /api/products` with a non-blank `q`
  on page 0; it is a zero-result search when the total is 0.
- Not counted: requests without a User-Agent or whose User-Agent matches crawler words (bot, crawl, spider,
  slurp, preview, facebookexternalhit, headless). Terms are lower-cased, trimmed, inner spaces collapsed, cut to
  60 characters. Terms holding 7 or more digits are dropped (they may be phone or order numbers).
- No IP, user id, session or User-Agent is stored. Rows older than 400 days are deleted by a daily job.
- Day = the Dhaka date when the request arrived.

### Reporting interfaces (read-only; they throw no expected errors)

- `order.api.SalesReport`: totals for a period (orders, units, revenue, delivery fees, cancelled, returned, new and
  returning buyers); daily series (Dhaka days); per-product sales (product id, orders, units, revenue); sales by
  district and by payment method. Native aggregate queries in `OrderRepository`.
- `cart.api.DemandReport` (new `api` package in cart): per product, wishlist count, units in members' carts and
  members with it in their cart.
- `catalog.api.CatalogReport`: product snapshot for every non-archived product (id, slug, name, category name,
  character name, status, stock, pre-order flag, created date); views per product in a period; total views and
  searches; top terms and top zero-result terms; early-access signup counts per coming-soon category.

Each returns small records from its own `api` package. Insights never imports an entity or repository.

### `ecommerce-insights` module

Depends on common, catalog, cart and order. It has no tables of its own. It merges the reports in memory. The
product list is the whole non-archived catalog (hundreds of rows at most; documented limit 2 000; past that it
needs SQL-side paging).

New privilege `INSIGHTS_READ` (V10 seeds it and grants it to ADMIN; added to `Privileges`).

| GET | Returns |
|---|---|
| `/api/admin/insights/overview?from&to` | totals + previous-period totals + change %, daily series, total views/searches |
| `/api/admin/insights/products?from&to&sort&dir` | every product row: sales, views, conversion (orders / views), wishlist, cart units, stock, days left, its signal types; sort by revenue, units, views, conversion, wishlist, daysLeft, stock |
| `/api/admin/insights/signals?from&to` | ranked signals, at most 30 |
| `/api/admin/insights/breakdown?from&to&by=CATEGORY\|CHARACTER\|DISTRICT\|PAYMENT_METHOD` | rows: name, orders, units, revenue, share of revenue |
| `/api/admin/insights/searches?from&to` | top 20 terms and top 20 zero-result terms with counts |

Signals (thresholds in `app.insights.*`, defaults below):

| Type | Rule | Ranked by |
|---|---|---|
| `RESTOCK_SOON` | in stock, selling, days left ≤ 14 | days left ascending |
| `WANTED_SOLD_OUT` | stock 0, not pre-order, wishlist + cart members ≥ 3 | demand |
| `LOW_STOCK_HIGH_DEMAND` | stock > 0, wishlist + cart members ≥ stock | demand / stock |
| `LOOKED_NOT_BOUGHT` | views ≥ 50, conversion < 1 % | views |
| `SLOW_MOVER` | stock > 0, no units sold in the period, product older than the period | stock |
| `SEARCH_GAP` | zero-result term searched ≥ 3 times | searches |
| `COMING_SOON_DEMAND` | coming-soon category with ≥ 1 early-access signup | signups |

Each signal: type, subject (product id + name + slug, term, or category), the numbers behind it, and a one-line
reason built from an `ErrorCode`-style message constant (no free text from data except names and terms).

Validation: `from ≤ to`, span ≤ 366 days, else 400 with new error codes. Every endpoint is `@PreAuthorize("hasAuthority('INSIGHTS_READ')")`.

## Frontend (`ecommerce_web`, built on `feat/storefront-v2-base`)

- `/admin/*`, a lazy-loaded chunk with its own layout (sidebar on desktop, top tabs on phone). It uses the
  `docs/DESIGN.md` tokens, but the screens are plainer and denser than the storefront.
- `/admin/sign-in`: staff email + password (`POST /api/auth/sign-in`). If `mustChangePassword`, the next screen is change
  password. `RequireAdmin` checks `roles` includes `ADMIN` (the UI only hides things; the backend enforces).
- **Dashboard**: period (7 / 30 / 90 days, custom); KPI tiles with change vs previous; a daily revenue and orders chart
  (inline SVG, no chart library); signals list linking to the product; breakdown tabs; search terms.
- **Products**: the insights product table, sortable, with an inline stock edit (`PATCH /api/admin/products/{id}/stock`,
  per option when the product has options).
- **Orders**: list with status filter and search, detail with timeline, items, address and the allowed actions with a note.
- Hooks in `src/api/admin.ts` via TanStack Query; response types in `src/api/types.ts`.

## Tests

- Backend unit: signal rules and their edges, days-left and conversion math (zero views, zero sales), period validation
  and previous-period dates, breakdown roll-up, `CatalogTraffic` (bot filter, digit filter, normalisation, page-0 rule,
  flush resets counters). `scripts/smoke_test.py`: views and a zero-result search show up after a flush; endpoints 403
  without the privilege.
- Frontend: Vitest for period maths and formatting, dashboard, product stock edit, order actions and the admin guard;
  one Playwright run through admin sign-in → dashboard → order action.

## Calls made while checking the design (Nayeem to confirm while reading)

- Pre-order products are left out of `RESTOCK_SOON`, `SLOW_MOVER` and `WANTED_SOLD_OUT` (their stock is an allocation).
- A set counts as its own product; its parts are not credited (sets have their own stock today).
- Products without a character roll up as "No character"; archived products are not in the product table, but their
  sales are in the totals and breakdowns.
- Signal thresholds are the defaults above, changeable in `application.properties` without a code change.
- Frontend work goes on `claude/ecstatic-wozniak-r0uwbi` re-based onto `feat/storefront-v2-base` (it has no commits of
  its own yet), so the admin screens sit on the v2 storefront.

## Left to the builder

Exact record and class names, query shapes, chart details, copy, how the flush interval is configured, the
`@Scheduled` job names.

## Known limits

- Views are approximate: bots that pretend to be browsers count, and a refetch counts again.
- Wishlist and cart demand are members only; guest carts live in the browser.
- Merging in memory caps out around 2 000 products.

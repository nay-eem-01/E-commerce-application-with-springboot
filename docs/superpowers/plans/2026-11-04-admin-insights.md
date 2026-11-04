# Admin insights dashboard: implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** demand signals (restock, sold-out demand, looked-not-bought, slow movers, search gaps, coming-soon demand),
sales overview, product table, breakdowns and search terms, as admin APIs and admin screens.

**Architecture:** catalog counts product views and searches in memory and flushes daily rows every minute (V10).
Order, cart and catalog each expose a read-only reporting interface in their `api` package; a new
`ecommerce-insights` module merges them and serves `/api/admin/insights/*` under `INSIGHTS_READ`. The web app gets
an `/admin` area on the v2 storefront: staff sign-in, dashboard, products with stock edit, orders with actions.

**Tech stack:** Spring Boot 3.5, Java 21, PostgreSQL 16, Flyway, JUnit 5 + Mockito + AssertJ; React 19 + TypeScript,
TanStack Query, Vitest + Testing Library, Playwright.

**Spec:** `docs/superpowers/specs/2026-11-04-admin-insights-design.md`

## Global constraints

- Dates and day buckets are Asia/Dhaka: `com.ecommerce.common.time.ShopTime.ZONE = ZoneId.of("Asia/Dhaka")`.
- Period: `from`/`to` inclusive local dates, default the last 30 days ending today, span ≤ 366 days, `from ≤ to`.
- Counted order: `placed_at` in the period and status not in (`CANCELLED`, `RETURNED`). Revenue = sum of `subtotal`.
- No personal data in counters or insights responses (no phone, email, IP, user id, User-Agent).
- No database write on a storefront request; counters flush every 60 s and on shutdown.
- Module rule: insights imports only `*.api` packages of catalog, cart and order, plus common.
- Money is `BigDecimal`; messages are `ErrorCode` constants; responses are records.
- Commits: conventional, small, author `nay-eem-01 <ahmedmdnayeem05@gmail.com>`, committer Claude, no attribution
  lines. Backend `./mvnw -B verify` and frontend `npm test && npm run lint && npm run build` before every push.
- Docs in the same branch: `docs/API.md`, `docs/ERD.md`, `docs/SECURITY.md`, `docs/ROADMAP.md`, `docs/DEV_LOG.md`
  (backend); `docs/ROADMAP.md`, `docs/DEV_LOG.md` (frontend).

## Review focus

1. Empty shop or empty period (no orders, no views): every endpoint answers 200 with zeros, no divide by zero, days
   left and conversion are null. Test: `InsightsServiceTest.emptyPeriodGivesZerosAndNulls`.
2. A product with views but archived later, or sales of an archived product: totals include it, product table and
   signals skip it, nothing throws on a missing snapshot. Test: `InsightsServiceTest.salesOfArchivedProductCountInTotalsOnly`.
3. A search page fetched again (page 1, 2 …) or a blank `q` must not count. Test: `CatalogTrafficTest.onlyFirstPageNonBlankSearchesCount`.
4. A flush that fails (database down) must keep the counts for the next try, not lose them, and never throw into the
   scheduler loop. Test: `CatalogTrafficTest.failedFlushKeepsCounts`.
5. A non-admin signed-in shopper opening `/admin` sees the sign-in screen, never a half-rendered dashboard; and a
   staff account with `mustChangePassword` cannot reach the dashboard. Test: `admin.test.tsx` guard cases.

---

## Backend (`E-commerce-application-with-springboot`, branch `claude/ecstatic-wozniak-r0uwbi`)

### Task 1: View and search counters (catalog, V10)

**Files:**
- Create: `ecommerce-api/src/main/resources/db/migration/V10__insights.sql`
- Create: `ecommerce-common/src/main/java/com/ecommerce/common/time/ShopTime.java`
- Create: `ecommerce-catalog/src/main/java/com/ecommerce/catalog/service/CatalogTraffic.java`
- Create: `ecommerce-catalog/src/main/java/com/ecommerce/catalog/repository/TrafficRepository.java` (JdbcTemplate/native upserts; no entities)
- Modify: `ecommerce-catalog/.../controller/StorefrontController.java` (product detail and list call the counter)
- Modify: `ecommerce-common/.../security/Privileges.java` (add `INSIGHTS_READ`)
- Test: `ecommerce-catalog/src/test/java/com/ecommerce/catalog/service/CatalogTrafficTest.java`

**Interfaces:**
- Produces: tables `product_view_daily(day, product_id, views)`, `search_term_daily(day, term, searches, zero_results)`;
  privilege `INSIGHTS_READ` seeded and granted to ADMIN in V10 (as V7 did for `NOTIFICATION_MANAGE`).
- Produces: `CatalogTraffic.productViewed(long productId, String userAgent)`,
  `CatalogTraffic.searched(String q, int pageNo, long total, String userAgent)`, `CatalogTraffic.flush()`
  (`@Scheduled(fixedDelayString = "${app.insights.flush-ms:60000}")`, also `@PreDestroy`),
  `CatalogTraffic.purgeOld()` (`@Scheduled(cron = "0 30 3 * * *", zone = "Asia/Dhaka")`, deletes days older than 400).
- Static helpers on `CatalogTraffic`: `static boolean isBot(String userAgent)`, `static Optional<String> normaliseTerm(String q)`.

- [ ] **Step 1: Write the failing tests** in `CatalogTrafficTest` (mock `TrafficRepository`, fixed `Clock`):
  - `botsAndMissingUserAgentAreNotCounted`: `isBot(null)`, `isBot("")`, `isBot("Googlebot/2.1")`, `isBot("facebookexternalhit/1.1")`, `isBot("HeadlessChrome")` are true; a Chrome desktop UA is false.
  - `termsAreNormalised`: `"  Kitsune   BOTTLE "` → `"kitsune bottle"`; a 90-char term is cut to 60; `"call 01712345678"` → empty; `"   "` → empty.
  - `onlyFirstPageNonBlankSearchesCount`: `searched("ramen", 1, 5, ua)` and `searched("", 0, 0, ua)` then `flush()` → repository gets no search rows.
  - `countsAreSummedPerDayAndFlushedOnce`: three views of product 7, one of 9, two searches for "ramen" with total 0 → one `upsertViews` call with `{7:3, 9:1}`, one `upsertSearches` with `ramen: searches 2, zeroResults 2`; a second `flush()` sends nothing.
  - `failedFlushKeepsCounts`: repository throws on first `upsertViews`; `flush()` does not throw; the next `flush()` sends `{7:3}`.
- [ ] **Step 2:** `./mvnw -B -pl ecommerce-catalog -am test -Dtest=CatalogTrafficTest -Dsurefire.failIfNoSpecifiedTests=false` → fails to compile.
- [ ] **Step 3:** implement. Counters keyed by `(LocalDate day, key)` where day = `LocalDate.now(clock.withZone(ShopTime.ZONE))`;
  swap the maps atomically on flush; on failure merge the swapped map back. Upserts: `INSERT ... ON CONFLICT (day, product_id) DO UPDATE SET views = product_view_daily.views + EXCLUDED.views`
  (same for terms). Skip product ids that no longer exist (`INSERT ... SELECT ... WHERE EXISTS`). Controller: add
  `@RequestHeader(value = "User-Agent", required = false) String userAgent` to `product` and `products`; call the counter after the service returns.
- [ ] **Step 4:** run the test → PASS; `./mvnw -B verify` → BUILD SUCCESS.
- [ ] **Step 5:** commit `feat(catalog): count product views and searches per day (V10)`.

### Task 2: Reporting interfaces for catalog and cart

**Files:**
- Create in `ecommerce-catalog/.../api/`: `CatalogReport.java`, `ProductSnapshot.java`, `TrafficTotals.java`, `SearchTermCount.java`, `EarlyAccessCount.java`
- Create: `ecommerce-catalog/.../service/CatalogReportImpl.java`; queries added to `ProductRepository`, `EarlyAccessSignupRepository`, `TrafficRepository`
- Create in `ecommerce-cart/.../api/`: `DemandReport.java`, `ProductDemand.java`; `ecommerce-cart/.../service/DemandReportImpl.java`; queries in `CartItemRepository`, `WishlistItemRepository`
- Test: `CatalogReportImplTest` (mapping of repository rows to records, limit passed through)

**Interfaces (produces):**
```java
record ProductSnapshot(Long id, String slug, String name, String category, String character,
                       boolean preorder, int stock, Instant createdAt) {}            // non-archived products only
record TrafficTotals(long views, long searches, long zeroResultSearches) {}
record SearchTermCount(String term, long searches, long zeroResults) {}
record EarlyAccessCount(String categorySlug, String categoryName, long signups) {}   // coming-soon categories only
interface CatalogReport {
    List<ProductSnapshot> products();
    Map<Long, Long> views(LocalDate from, LocalDate to);                            // inclusive days
    TrafficTotals totals(LocalDate from, LocalDate to);
    List<SearchTermCount> topSearches(LocalDate from, LocalDate to, int limit);      // by searches desc
    List<SearchTermCount> topZeroResultSearches(LocalDate from, LocalDate to, int limit); // zero_results > 0, by zero_results desc
    List<EarlyAccessCount> earlyAccess();
}
record ProductDemand(Long productId, long wishlists, long cartUnits, long cartMembers) {}
interface DemandReport { Map<Long, ProductDemand> byProduct(); }
```
All implementations `@Transactional(readOnly = true)`. `character` is null when the product has none.

- [ ] Step 1: failing `CatalogReportImplTest` (rows → records; `topSearches(…, 20)` passes 20). Step 2: run → fails.
  Step 3: implement (JPQL constructor projections or native queries returning interfaces). Step 4: `./mvnw -B verify` → SUCCESS.
  Step 5: commit `feat(catalog,cart): read-only reporting interfaces for insights`.

### Task 3: Sales report (order)

**Files:**
- Create in `ecommerce-order/.../api/`: `SalesReport.java`, `SalesTotals.java`, `DailySales.java`, `ProductSales.java`, `GroupSales.java`
- Create: `ecommerce-order/.../service/SalesReportImpl.java`; native aggregate queries in `OrderRepository`
- Test: `ecommerce-order/src/test/java/com/ecommerce/order/service/SalesReportImplTest.java`

**Interfaces (produces):** `from` inclusive, `to` exclusive instants (callers pass Dhaka day starts).
```java
record SalesTotals(long orders, long units, BigDecimal revenue, BigDecimal deliveryFees,
                   long cancelled, long returned, long newBuyers, long returningBuyers) {}
record DailySales(LocalDate day, long orders, BigDecimal revenue) {}   // only days that had orders
record ProductSales(Long productId, long orders, long units, BigDecimal revenue) {}  // revenue = sum(line_total)
record GroupSales(String key, long orders, long units, BigDecimal revenue) {}
interface SalesReport {
    SalesTotals totals(Instant from, Instant to);
    List<DailySales> daily(Instant from, Instant to);
    List<ProductSales> byProduct(Instant from, Instant to);
    List<GroupSales> byDistrict(Instant from, Instant to);
    List<GroupSales> byPaymentMethod(Instant from, Instant to);
}
```
New buyer: phone whose earliest counted order (`min(placed_at)` over all time, status not cancelled/returned) is ≥ `from`.
Day bucket: `(placed_at AT TIME ZONE 'Asia/Dhaka')::date`. Null sums map to `BigDecimal.ZERO`.

- [ ] Step 1: failing `SalesReportImplTest`: `nullAggregatesBecomeZero` (repository row with null revenue → `BigDecimal.ZERO`, 0 counts).
  Step 2: run → fails. Step 3: implement. Step 4: `./mvnw -B verify`. Step 5: commit `feat(order): sales report for insights`.
  (The SQL itself is checked against PostgreSQL in Task 5.)

### Task 4: Insights module and endpoints

**Files:**
- Create module `ecommerce-insights` (`pom.xml` depends on common, catalog, cart, order); add to root `pom.xml` modules and dependencyManagement, and as a dependency of `ecommerce-api`.
- Create: `insights/config/InsightsProperties.java` (`@ConfigurationProperties("app.insights")`, record with defaults:
  `restockDays 14`, `wantedSoldOutMin 3`, `lookedViewsMin 50`, `lookedConversionMax 0.01`, `searchGapMin 3`, `signalLimit 30`, `productLimit 2000`)
- Create: `insights/service/Period.java` (`record Period(LocalDate from, LocalDate to)`, `static Period of(LocalDate from, LocalDate to, Clock clock)`, `int days()`, `Period previous()`, `Instant start()`, `Instant endExclusive()`)
- Create: `insights/service/InsightsService.java`, `insights/service/Signals.java` (pure rules)
- Create: `insights/controller/AdminInsightsController.java`
- Create: `insights/dto/response/`: `OverviewResponse`, `TotalsResponse`, `ProductInsightResponse`, `SignalResponse`, `BreakdownRowResponse`, `SearchesResponse`
- Create: `insights/enums/`: `SignalType` (7 values from the spec), `BreakdownBy` (`CATEGORY, CHARACTER, DISTRICT, PAYMENT_METHOD`)
- Modify: `ErrorCode` (`INVALID_PERIOD = "The start date must be on or before the end date."`, `PERIOD_TOO_LONG = "Choose a period of 366 days or less."`); `application.properties` (`app.insights.*` defaults)
- Test: `PeriodTest`, `SignalsTest`, `InsightsServiceTest`

**Interfaces:**
- Consumes: `CatalogReport`, `DemandReport`, `SalesReport` (Tasks 2–3).
- Produces (JSON contract for the frontend):
```java
record TotalsResponse(long orders, long units, BigDecimal revenue, BigDecimal averageOrder, BigDecimal deliveryFees,
                      long cancelled, BigDecimal cancelRate, long returned, long newBuyers, long returningBuyers,
                      long views, long searches, long zeroResultSearches) {}
record OverviewResponse(LocalDate from, LocalDate to, TotalsResponse current, TotalsResponse previous,
                        List<DailySales> daily) {}          // daily filled with zero days, one row per day
record ProductInsightResponse(Long id, String slug, String name, String category, String character, boolean preorder,
                              int stock, long views, long orders, long units, BigDecimal revenue,
                              BigDecimal conversion, long wishlists, long cartUnits, long cartMembers,
                              BigDecimal daysLeft, List<SignalType> signals) {}
record SignalResponse(SignalType type, String title, String reason, Long productId, String productSlug,
                      String term, String categorySlug, long value) {}
record BreakdownRowResponse(String name, long orders, long units, BigDecimal revenue, BigDecimal share) {}
record SearchesResponse(List<SearchTermCount> top, List<SearchTermCount> zeroResults) {}
```
  `cancelRate = cancelled / (orders + cancelled)` (0–1, 4 decimals); `conversion = orders / views` (null when views 0);
  `averageOrder = revenue / orders` (null when 0); `daysLeft = stock / (units / days)`, 1 decimal, null when units 0;
  `share = revenue / total revenue`, 4 decimals. Rounding `HALF_UP`.
- Endpoints (all `@PreAuthorize("hasAuthority('INSIGHTS_READ')")`, `from`/`to` optional ISO dates):
  `GET /api/admin/insights/overview`, `/products?sort=revenue|units|views|conversion|wishlists|daysLeft|stock&dir=desc`,
  `/signals`, `/breakdown?by=`, `/searches`. Unknown sort → 400 `INVALID_SORT_FIELD`.
- Signal rules as in the spec table; pre-order products skip `RESTOCK_SOON`, `SLOW_MOVER`, `WANTED_SOLD_OUT`;
  `SLOW_MOVER` needs `createdAt` before `period.start()`. Order: by type in the spec's table order, then by its rank
  value; cut at `signalLimit`. `title`/`reason` are built from constants in `Signals` with names, terms and numbers
  (e.g. `"%s: about %s days of stock left"`).

- [ ] **Step 1: failing tests**
  - `PeriodTest`: defaults to the 30 days ending today in Dhaka (clock at `2026-11-04T20:00Z` → to `2026-11-05`); `from > to` → `ResponseException` 400 `INVALID_PERIOD`; 367 days → `PERIOD_TOO_LONG`; `previous()` of 11-01..11-30 is 10-02..10-31; `start()` of 11-01 is `2026-10-31T18:00:00Z`.
  - `SignalsTest`: one test per signal at its threshold and just below it (e.g. `restockSoonAt14DaysNotAt15`, `wantedSoldOutNeedsThreeAndNotPreorder`, `lookedNotBoughtAt50ViewsUnder1Percent`, `slowMoverOnlyIfOlderThanPeriod`, `searchGapAtThreeSearches`, `comingSoonNeedsOneSignup`); `limitCutsAt30`.
  - `InsightsServiceTest`: `emptyPeriodGivesZerosAndNulls`, `salesOfArchivedProductCountInTotalsOnly`, `dailySeriesFillsMissingDays`, `breakdownByCharacterRollsUpAndNamesNoCharacter`, `productsSortByDaysLeftPutsNullsLast`, `unknownSortIs400`.
- [ ] **Step 2:** `./mvnw -B -pl ecommerce-insights -am test` → fails to compile.
- [ ] **Step 3:** implement. Breakdown CATEGORY/CHARACTER: roll `byProduct` up through `products()` (archived → "Archived"; no character → "No character"). DISTRICT/PAYMENT_METHOD come from the sales report.
- [ ] **Step 4:** `./mvnw -B verify` → SUCCESS.
- [ ] **Step 5:** commit `feat(insights): dashboard endpoints with signals, breakdowns and searches`.

### Task 5: End-to-end check against PostgreSQL, docs

**Files:**
- Modify: `scripts/smoke_test.py` (insights section after the admin flow)
- Modify: `docs/API.md`, `docs/ERD.md`, `docs/SECURITY.md`, `docs/ROADMAP.md` (new step 4.5a "Admin insights backend"), `docs/DEV_LOG.md`

- [ ] Step 1: add checks: customer token on `/api/admin/insights/overview` → 403; view a product twice and search `"zzqx"` (zero results) with a browser UA, wait 65 s (or run the app with `APP_INSIGHTS_FLUSH_MS=2000` and wait 3 s), then admin `/searches` lists `zzqx` in `zeroResults` and `/products` shows that product with `views >= 2`; `/overview` totals equal the count of non-cancelled orders the script placed today; `/signals` and `/breakdown?by=DISTRICT` answer 200; `from=2026-12-01&to=2026-11-01` → 400.
- [ ] Step 2: start PostgreSQL 16 locally, fresh database, run the app (development profile) and the script → all checks pass. Fix SQL found wrong; each fix gets a regression unit test where testable.
- [ ] Step 3: docs. Step 4: `./mvnw -B verify`. Step 5: commit `test: insights checks in the end-to-end script` and `docs: insights endpoints, counters and privilege`; push.

---

## Frontend (`ecommerce_web`, branch `claude/ecstatic-wozniak-r0uwbi` reset onto `origin/feat/storefront-v2-base`)

### Task 6: Admin shell, staff sign-in and guard

**Files:**
- Create: `src/features/admin/AdminLayout.tsx`, `AdminSignInPage.tsx`, `ChangePasswordPage.tsx`, `RequireAdmin.tsx`, `admin.test.tsx`
- Create: `src/api/admin.ts` (hooks), types appended to `src/api/types.ts`
- Modify: `src/App.tsx` (lazy `/admin/*` routes outside `AppLayout`), `src/features/auth/session.tsx` if it needs a staff sign-in method

**Interfaces (produces):** routes `/admin` (dashboard), `/admin/products`, `/admin/orders`, `/admin/orders/:orderNumber`,
`/admin/sign-in`, `/admin/password`. `useStaffSignIn()` (POST `/api/auth/sign-in`), `useChangePassword()`.
`RequireAdmin`: signed out or no `ADMIN` role → `/admin/sign-in?back=`; `mustChangePassword` → `/admin/password`.

- [ ] Step 1: failing tests in `admin.test.tsx`: guest → sign-in screen; customer session → sign-in screen with "This account has no staff access"; admin with `mustChangePassword` → change-password screen; admin → layout with nav "Dashboard, Products, Orders"; wrong password shows the backend's message.
- [ ] Step 2: `npm test -- admin` → fails. Step 3: implement (reuse the session store and API client; same token rules). Step 4: `npm test && npm run lint && npm run build`. Step 5: commit `feat(admin): staff sign-in, password change and admin layout`.

### Task 7: Dashboard

**Files:**
- Create: `src/features/admin/DashboardPage.tsx`, `PeriodPicker.tsx`, `KpiTiles.tsx`, `DailyChart.tsx` (inline SVG), `SignalsList.tsx`, `Breakdown.tsx`, `SearchTerms.tsx`, `insightsFormat.ts`, `dashboard.test.tsx`
- Modify: `src/api/admin.ts` (`useOverview(period)`, `useSignals(period)`, `useBreakdown(period, by)`, `useSearches(period)`)

**Interfaces:** `type Period = { from: string; to: string }` (ISO dates); `periodFor(days: 7 | 30 | 90, today: Date): Period`
(Dhaka today); `changePct(current: number, previous: number): number | null` (null when previous is 0).
Period lives in the URL (`?from=&to=`).

- [ ] Step 1: failing tests: `periodFor(30, 2026-11-04T20:00Z)` → `{from: '2026-10-07', to: '2026-11-05'}`; `changePct(120, 100) = 20`, `changePct(5, 0) = null`; dashboard renders tiles from a mocked overview (revenue as `৳ 12,400`), the signals list links `RESTOCK_SOON` to `/admin/products?highlight=<slug>`, an empty period shows "No orders in this period" instead of a chart, breakdown tab switch requests `by=CHARACTER`.
- [ ] Step 2 → fail. Step 3: implement (follow `dataviz` skill for the chart: one series bars for revenue, order count in the tooltip/label; works in light and dark). Step 4: tests, lint, build. Step 5: commit `feat(admin): insights dashboard`.

### Task 8: Products with stock edit

**Files:** create `src/features/admin/ProductsPage.tsx`, `StockEditor.tsx`, `products.test.tsx`; modify `src/api/admin.ts`
(`useProductInsights(period, sort, dir)`, `useAdminProduct(id)` for options, `useUpdateStock()` → `PATCH /api/admin/products/{id}/stock {optionId?, stock}`, invalidates product insights).

- [ ] Step 1: failing tests: table sorts by clicking a header (requests `sort=units`); signal badges per row; stock editor for a product with options lists each option and sends `optionId`; a 400 shows the backend message; `?highlight=slug` scrolls to and marks that row.
- [ ] Steps 2–5 as above; commit `feat(admin): product insights table with stock edit`.

### Task 9: Orders

**Files:** create `src/features/admin/OrdersPage.tsx`, `AdminOrderPage.tsx`, `orders.test.tsx`; modify `src/api/admin.ts`
(`useAdminOrders(status, q, page)`, `useAdminOrder(orderNumber)`, `useOrderAction()` → `PATCH /api/admin/orders/{n}/status {action, note?}`).
Allowed buttons per status: PLACED → PACK, CANCEL; PACKED → SHIP, CANCEL; SHIPPED → DELIVER, RETURN; DELIVERED → RETURN; else none (the backend still decides; a 409 shows its message).

- [ ] Step 1: failing tests: status filter and search update the URL and query; detail shows timeline, items, address; CANCEL asks for confirmation and sends the note; 409 message shown.
- [ ] Steps 2–5; commit `feat(admin): order list, detail and status actions`.

### Task 10: End-to-end and docs

- [ ] Add `e2e/admin.spec.ts`: admin signs in (password from `E2E_ADMIN_PASSWORD`), sees the dashboard, opens an order placed by the shop test and packs it. Run against the backend from Task 5. Update `docs/ROADMAP.md` (Phase 4 admin step), `docs/DEV_LOG.md`, `docs/BACKEND_REQUESTS.md` if anything is missing. Commit `test(e2e): admin sign-in, dashboard and order action` and `docs: admin dashboard`; push.

---

## After the build (asked for by Nayeem)

1. `code-review-and-quality` over both branches; fix what it finds (each fix with a test).
2. `security-audit` over both repositories, focused on the new surface (privilege checks, counters as an abuse vector, admin sign-in, personal data); fix or record in `docs/SECURITY.md`.

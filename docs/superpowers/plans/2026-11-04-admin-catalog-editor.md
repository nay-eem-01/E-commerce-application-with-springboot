# Admin catalog editor and image uploads: implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** staff upload product images (stored in S3, served as WebP) and run products, categories, characters, users
and reviews from `/admin`.

**Architecture:** a `media` package in `ecommerce-catalog` checks, converts and stores images behind an
`ImageStorage` interface (S3 or local disk by property), ported from AgroTrends. The catalog adds an edit view of a
product and enforces uploaded-only media URLs; the review module adds an admin list. The web app adds the editor and
three management screens on the existing admin layout.

**Tech stack:** Spring Boot 3.5, Java 21, AWS SDK v2 `s3`, `com.github.usefulness:webp-imageio:0.11.0`;
React 19, TanStack Query, Vitest, Playwright.

**Spec:** `docs/superpowers/specs/2026-11-04-admin-catalog-editor-design.md`

## Global constraints

- Upload: JPEG, PNG or WebP by first bytes only; at most 5 MB (`5L * 1024 * 1024`); multipart limits 6 MB file / 7 MB
  request. Sides ≤ 8 000 px and ≤ 40 000 000 pixels, checked before decoding.
- JPEG/PNG → lossy WebP quality `0.82f`, longest side scaled down to ≤ 2 400 px (never up). WebP stored as is.
- Stored name `<uuid>.webp` (or the upload's `.webp`), content type `image/webp`,
  `Cache-Control: public, max-age=31536000, immutable`.
- Env: `S3_BUCKET`, `S3_REGION`, `S3_ENDPOINT` (optional), `S3_ACCESS_KEY_ID`, `S3_SECRET_ACCESS_KEY`,
  `UPLOADS_PUBLIC_BASE_URL`, `STORAGE_DIR` (default `uploads`). No defaults for secrets. Blank bucket → local disk;
  local disk in the `production` profile fails startup.
- Upload endpoint `POST /api/admin/uploads`, `PRODUCT_WRITE`, 60 per minute per user.
- Errors are `ErrorCode` constants; responses are records; schema changes only by Flyway (none expected).
- Commits: author `nay-eem-01 <ahmedmdnayeem05@gmail.com>`, committer Claude, no attribution lines. Backend
  `./mvnw -B verify`; frontend `npm test && npm run lint && npm run build`, before every push. Docs (`API.md`,
  `SECURITY.md`, `ROADMAP.md`, `DEV_LOG.md`, `.env.example`; frontend `ROADMAP.md`, `DEV_LOG.md`) in the same branch.
- Branches: backend `claude/ecstatic-wozniak-r0uwbi` from `main`; frontend `claude/ecstatic-wozniak-r0uwbi` restarted
  from `feat/storefront-v2-base`.

## Review focus

1. A CMYK or 16-bit PNG/JPEG the JDK can read but not as RGB: conversion must still produce WebP or answer 400
   `IMAGE_UNREADABLE`, never 500. Test: `ImageServiceTest.unreadableImageIs400`.
2. Editing an old product that has a media URL from another host must save; adding that same foreign URL to a
   different product must not. Test: `ProductAdminServiceTest.keepsExistingForeignUrlButRejectsItElsewhere`.
3. An editor save after changing one field sends the same options, stock, media order and tags it loaded.
   Test: `editor.test.tsx` "saves the document it loaded with only the edited field changed".
4. Uploading 36 frames where one fails: the other 35 keep their order and the failed one can be retried in place.
   Test: `editor.test.tsx` "a failed upload stays in place and can be retried".
5. A slug clash or a 400 on a hidden section (say an option row) must show next to that field and scroll there, not
   only in a banner. Test: `editor.test.tsx` "shows field errors from the backend beside the field".

---

## Backend

### Task 1: Image check, conversion and storage

**Files:**
- Create in `ecommerce-catalog/src/main/java/com/ecommerce/catalog/media/`: `ImageType.java`, `ImageService.java`,
  `ImageStorage.java`, `StorageProperties.java`, `S3ImageStorage.java`, `LocalDiskImageStorage.java`, `UploadsResourceConfig.java`
- Create: `ecommerce-catalog/.../controller/AdminUploadController.java`
- Modify: `ecommerce-catalog/pom.xml` (AWS SDK `software.amazon.awssdk:s3` with the BOM version pinned in the root
  `pom.xml` `dependencyManagement`, and `com.github.usefulness:webp-imageio:0.11.0`), `ErrorCode`
  (`IMAGE_REQUIRED`, `IMAGE_TOO_LARGE`, `IMAGE_TYPE_NOT_ALLOWED`, `IMAGE_TOO_MANY_PIXELS`, `IMAGE_UNREADABLE`,
  `IMAGE_NOT_UPLOADED`), `application.properties`, `.env.example`, `SecurityConfig` (public GET `/uploads/**`)
- Test: `ecommerce-catalog/src/test/java/com/ecommerce/catalog/media/ImageTypeTest.java`, `ImageServiceTest.java`,
  `StoragePropertiesTest.java`

**Interfaces (produces):**
```java
enum ImageType { JPEG, PNG, WEBP; static Optional<ImageType> detect(byte[] bytes); }
/** Width and height of a WebP from its RIFF header (VP8 , VP8L, VP8X chunks); empty when malformed. */
static Optional<int[]> ImageType.webpSize(byte[] bytes);
interface ImageStorage { String store(byte[] webp); boolean isStored(String url); }
record StorageProperties(String localDir, String publicBaseUrl, S3 s3)   // @ConfigurationProperties("app.storage")
  record S3(String bucket, String region, String endpoint, String accessKeyId, String secretAccessKey)
  String newFileName();                 // UUID + ".webp"
  String publicUrl(String fileName);
  boolean isPublicUrl(String url);      // <base>/<uuid>.webp only
class ImageService {
  static final long MAX_BYTES = 5L * 1024 * 1024; static final int MAX_SIDE = 2400, LIMIT_SIDE = 8000;
  static final long LIMIT_PIXELS = 40_000_000L; static final float QUALITY = 0.82f;
  String upload(byte[] bytes, long userId);      // checks, rate limit "upload:" + userId 60/min, converts, stores
  byte[] toWebp(byte[] bytes, ImageType type);   // package-visible, for tests
}
```
`AdminUploadController`: `POST /api/admin/uploads` (multipart `file`), `@PreAuthorize("hasAuthority('PRODUCT_WRITE')")`,
returns `Response.created("Image uploaded.", Map.of("url", url))`.

- [ ] **Step 1: failing tests**
  - `ImageTypeTest`: JPEG/PNG/WebP signatures detected; a PNG body with a `.jpg` name still PNG (name never read);
    `<svg` bytes, empty, 3-byte input → empty; `webpSize` reads a lossy (VP8), lossless (VP8L) and extended (VP8X)
    header built in the test.
  - `ImageServiceTest` (storage and rate limiter mocked; images drawn with `BufferedImage` in the test):
    `emptyIs400`, `over5MbIs400`, `textFileIs400` (`IMAGE_TYPE_NOT_ALLOWED`), `pngBecomesWebpAtMost2400` (a 3000×1500
    PNG → WebP whose decoded size is 2400×1200), `smallJpegIsNotScaledUp` (800×600 stays 800×600),
    `tooManyPixelsIs400WithoutDecoding` (a PNG header claiming 9000×9000 → `IMAGE_TOO_MANY_PIXELS`),
    `webpIsStoredUnchanged` (same bytes reach `store`), `unreadableImageIs400` (truncated PNG after a valid header →
    `IMAGE_UNREADABLE`), `sixtyFirstUploadInAMinuteIs429`.
  - `StoragePropertiesTest`: `isPublicUrl` true only for `<base>/<uuid>.webp` (trailing slashes in base ignored;
    another host, `..`, `.png`, query string → false); `newFileName` ends `.webp`; bucket set without keys or region
    or base URL → startup validation fails.
- [ ] **Step 2:** `./mvnw -B -pl ecommerce-catalog -am test -Dsurefire.failIfNoSpecifiedTests=false -Dtest='ImageTypeTest,ImageServiceTest,StoragePropertiesTest'` → compile failure.
- [ ] **Step 3:** implement. Dimensions: `ImageIO.getImageReaders(stream)` → `setInput(stream, true, true)` →
  `getWidth(0)`/`getHeight(0)` before `read`. Convert to `TYPE_INT_RGB` (or `TYPE_INT_ARGB` when the source has
  alpha) before encoding; any `IOException`/`IllegalArgumentException` from decode → 400 `IMAGE_UNREADABLE`. Storage
  selection by `@ConditionalOnExpression` on the bucket, as AgroTrends. `LocalDiskImageStorage` throws at startup in
  the `production` profile. Properties: `app.storage.local-dir=${STORAGE_DIR:uploads}`,
  `app.storage.public-base-url=${UPLOADS_PUBLIC_BASE_URL:http://localhost:8080/uploads}`,
  `app.storage.s3.*=${S3_*:}`, multipart limits.
- [ ] **Step 4:** tests pass; `./mvnw -B verify` passes.
- [ ] **Step 5:** commit `feat(catalog): image upload with WebP conversion, S3 or local storage`.

### Task 2: Uploaded-only media, edit view, review list

**Files:**
- Modify: `ProductAdminService` (media check, `editView`), `AdminCatalogController` (`GET /products/{id}/edit`)
- Create: `ecommerce-catalog/.../dto/response/ProductEditResponse.java`
- Create: `ecommerce-review/.../dto/response/AdminReviewResponse.java`; modify `ReviewService`, `ReviewRepository`,
  `ReviewController` (`GET /api/admin/reviews`)
- Test: `ProductAdminServiceTest.java` (new), `ReviewServiceTest` (add)

**Interfaces (produces):**
```java
record ProductEditResponse(Long id, String slug, Long categoryId, Long characterId, String name, String maker,
    String summary, String description, String story, String storyQuote, String specLine, BigDecimal price,
    BigDecimal compareAtPrice, int stock, boolean preorder, String preorderRelease, BigDecimal deposit, boolean sized,
    ProductStatus status, String optionName, List<Option> options, List<Fact> facts, List<String> boxItems,
    List<Media> media, List<Long> bundleProductIds, List<Tag> tags) {
  record Option(Long id, String value, BigDecimal priceDelta, int stock, String swatch) {}
  record Fact(String label, String value) {}
  record Media(MediaKind kind, String url, String altText) {}   // in saved order
  record Tag(String group, String value) {}
}
record AdminReviewResponse(Long id, Long productId, String productName, String authorName, int rating, String body,
    ReviewStatus status, Instant createdAt) {}
```
`GET /api/admin/reviews?status=&productId=&pageNo&pageSize` (`REVIEW_MODERATE`), newest first, `PageResponse`.
Product names via `CatalogApi.summaries` (archived products: name from a new `CatalogApi.names(Collection<Long>)`
returning id → name for any status). Media rule in `replaceMedia`: URL kept if it is already on this product,
else `imageStorage.isStored(url)` or 400 `IMAGE_NOT_UPLOADED`.

- [ ] Step 1: failing tests: `ProductAdminServiceTest.keepsExistingForeignUrlButRejectsItElsewhere`,
  `acceptsUploadedUrlOnCreate`, `editViewRoundTripsWithoutChangingStock` (build a product with two options of stock 4
  and 0, map to `ProductEditResponse`, turn it into a `ProductRequest`, `update` → stock still 4 and 0, media order
  and tags unchanged); `ReviewServiceTest.adminListFiltersByStatusAndNamesArchivedProducts`.
  Step 2: run → fail. Step 3: implement. Step 4: `./mvnw -B verify`. Step 5: commit
  `feat(catalog,review): product edit view, uploaded-only media, admin review list`.

### Task 3: End-to-end checks and docs (backend)

- [ ] Add to `scripts/smoke_test.py` (local storage): upload a 1200×900 PNG → 201, URL ends `.webp`, `GET` it →
  200 `image/webp`; upload text → 400; customer token → 403; create a product with that URL → 201; a foreign URL →
  400 with `IMAGE_NOT_UPLOADED`; edit view → PUT it back → stock unchanged; `/api/admin/reviews` → 200 and no `phone`
  key. Run on a fresh development database → all pass.
- [ ] Docs: `API.md` (uploads, edit view, reviews), `SECURITY.md` (S6 replaced by the controls), `ROADMAP.md` (tick 3.4,
  add 4.5c steps), `DEV_LOG.md`, `.env.example`. Commit `test: uploads and editor checks in the end-to-end script`,
  `docs: uploads, edit view and review list`; push.

## Frontend

### Task 4: Product editor with uploads

**Files:** create `src/features/admin/editor/ProductEditorPage.tsx`, `ImageList.tsx`, `OptionRows.tsx`,
`productForm.ts` (`toRequest`, `fromEdit`, `slugify`), `editor.test.tsx`, `productForm.test.ts`; modify
`src/api/admin.ts` (`useProductEdit(id)`, `useSaveProduct()`, `uploadImage(file): Promise<string>`,
`useCategories()`, `useCharacters()`), `src/api/types.ts` (`ProductEdit`, `ProductRequestBody`), `App.tsx` routes
`products/new`, `products/:id`, `ProductsPage.tsx` ("New product", "Edit" per row).

**Interfaces:** `uploadImage` posts `FormData` with `file` through `api()` (it must not set `Content-Type` for
FormData; extend `client.ts` to pass `FormData` bodies untouched). `slugify("Kitsune Night bottle, 750 ml")` →
`"kitsune-night-bottle-750-ml"`. Upload concurrency 3. Reorder by native HTML drag and drop plus move-up/move-down buttons (no new dependency).
"Archive" on an existing product calls `DELETE /api/admin/products/{id}` after a confirmation. Spin frames sorted by file name with `localeCompare(…,
{numeric: true})` on pick.

- [ ] Step 1: failing tests: `productForm.test.ts` (`slugify` cases; `toRequest(fromEdit(x))` deep-equals the
  request built from `x`); `editor.test.tsx`: "saves the document it loaded with only the edited field changed",
  "creates a product and goes to its edit page", "shows field errors from the backend beside the field",
  "uploads many frames in file-name order", "a failed upload stays in place and can be retried", "moves an image with
  the keyboard buttons", "asks before leaving with unsaved changes".
- [ ] Steps 2–5: fail, implement, `npm test && npm run lint && npm run build`, commit
  `feat(admin): product editor with image upload`.

### Task 5: Categories and characters

**Files:** create `src/features/admin/CatalogPage.tsx` (two tables), `catalog.test.tsx`; hooks in `admin.ts`
(`useSaveCategory`, `useDeleteCategory`, `useSaveCharacter`, `useDeleteCharacter`); route `/admin/catalog`; nav item
"Catalog".

- [ ] Tests: create a category (body has slug, name, label, blurb, sortOrder, active, comingSoon); edit inline; delete
  asks first and shows the backend's 409 message when the category still has products; same for characters.
- [ ] Implement, check, commit `feat(admin): categories and characters`.

### Task 6: Users and reviews; end-to-end; docs

**Files:** create `src/features/admin/UsersPage.tsx`, `ReviewsPage.tsx`, `people.test.tsx`; hooks
`useAdminUsers(q, page)`, `useSetUserStatus()`, `useAdminReviews(status, page)`, `useSetReviewStatus()`; routes
`/admin/users`, `/admin/reviews`; nav items "Users", "Reviews"; `e2e/admin.spec.ts` (add: create a product with an
uploaded photo from `e2e/fixtures/photo.png`, open `/p/<slug>`, see the image).

- [ ] Tests: search sends `q`; ban asks first and sends `{status: 'BANNED'}`; the backend's "cannot suspend your own
  account" shows; reviews filter by status and hide sends `{status: 'HIDDEN'}`.
- [ ] Implement, check, run Playwright against the backend from Task 3, docs (`ROADMAP.md` phase 4 rows, `DEV_LOG.md`),
  commit `feat(admin): users and reviews` and `test(e2e): product with an uploaded photo`, push.

## After the build

`code-review-and-quality` over both branches and a focused security pass on the upload path (byte checks, limits,
storage selection, URL rule); fix what they find; open the two PRs on request.

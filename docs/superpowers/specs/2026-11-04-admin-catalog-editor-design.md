# Admin catalog editor and image uploads: design

Date: 2026-11-04. Roadmap 3.4 (image upload) and 4.5c (full product editor, categories, characters, users,
reviews). Covers the backend (this repository) and the admin screens in `ecommerce_web`.

## Intent

Staff run the whole catalog from `/admin` instead of Swagger: create and edit products with real photos and 360
turntables, keep categories and characters, ban abusive accounts and hide bad reviews. Images are uploaded to the
backend, stored in S3, and served as WebP so the shop's pages load fast on Bangladeshi mobile data.

## Goals

- `POST /api/admin/uploads`: one image (JPEG, PNG or WebP, at most 5 MB) in, a public WebP URL out.
- Storage in S3 (or any S3-compatible store: R2, MinIO) in production; local disk in development.
- Product create and edit in the web app with every field the backend's `ProductRequest` takes, including option
  stock, facts, box items, tags, set contents, status and archive, and three image lists (photos, 360 spin frames,
  story image) with multi-file upload and reordering.
- Categories and characters: list, create, edit, delete.
- Users: search, ban, unban. Reviews: list by status or product, hide, publish.

## Non-goals

- Deleting uploads nobody uses any more (orphans stay in the bucket; a cleanup job is later).
- Image cropping or editing in the browser; several sizes per image (`srcset`).
- Staff accounts and roles beyond the one ADMIN role.
- Rich text in descriptions (product text stays plain text with blank-line paragraphs).

## Anti-goals

- A save that silently changes something staff did not touch (wipes option stock, drops the character, reorders
  images).
- Product pages that point at images on hosts we do not control.
- Untrusted bytes reaching native code.

## Approaches

| | Approach | Trade-off | Decision |
|---|---|---|---|
| Images | Paste image web addresses | No backend work; staff must host images, 36 pasted addresses per turntable | Rejected by Nayeem |
| | **Upload to the backend, stored in S3, as AgroTrends does** | Storage and credentials needed; one pick of 36 files | **Picked** (Nayeem: "see AgroTrends") |
| WebP | **Convert on the server** (`com.github.usefulness:webp-imageio` 0.11.0, Apache-2.0, bundled libwebp) | Adds one library (about 8 MB of natives for every OS) and `kotlin-stdlib`; tried in this session: PNG and JPEG encode to WebP and decode back at full size | **Picked** (my call) |
| | Convert in the browser (`canvas.toBlob('image/webp')`) | No backend library, but Safari does not encode WebP, and the server still has to trust the client's bytes | Rejected |
| | Store originals, convert at a CDN (image resizing service) | Best quality, but ties us to a provider and its pricing | Later, if needed |

## Image pipeline (backend)

Ported from AgroTrends (`development` branch: `ImageStorage`, `ImageType`, `StorageProperties`,
`LocalDiskImageStorage`, `R2ImageStorage`, `ImageService`, `UploadsResourceConfig`), adapted:

1. **Check.** Empty → 400. Over 5 MB → 400 (also `spring.servlet.multipart.max-file-size=6MB`, request 7 MB). The
   type comes from the first bytes only (`ImageType.detect`): JPEG, PNG, WebP; anything else → 400.
2. **Dimensions before decoding.** For JPEG and PNG, read width and height from the header with an `ImageReader`
   (no pixel decode). Reject a side over 8 000 px or more than 40 megapixels (decompression bombs). My call.
3. **JPEG and PNG → WebP.** Decode with the JDK's own ImageIO (pure Java), scale down so the longest side is at most
   2 400 px (keep aspect, never scale up), encode lossy WebP at quality 0.82 with libwebp. Only our decoded pixels
   reach the native encoder. Alpha is kept for PNG.
4. **WebP uploads are stored as they are**, after the byte check, never decoded on the server (that would put
   untrusted bytes into native code). Its dimensions are read from the RIFF header (VP8, VP8L or VP8X chunk) and
   checked against the same limits. My call.
5. **Store** under a random name `<uuid>.webp`, content type `image/webp`, `Cache-Control: public, max-age=31536000,
   immutable`. Return `{url}` built from `UPLOADS_PUBLIC_BASE_URL`.

Storage classes, chosen by property exactly as AgroTrends does (blank bucket → local disk):

- `S3ImageStorage`: AWS SDK v2 `S3Client`, region from `S3_REGION`, optional `S3_ENDPOINT` (R2, MinIO), static
  credentials from `S3_ACCESS_KEY_ID` / `S3_SECRET_ACCESS_KEY`. Startup fails when the bucket is set and a key, the
  region or the public base URL is missing. No ACL is set on objects: the bucket policy or CDN makes them public.
- `LocalDiskImageStorage`: `STORAGE_DIR` (default `uploads`), served read-only at `/uploads/**`, development only
  (startup fails in production without a bucket).

Where it lives: a `media` package inside `ecommerce-catalog` (`media.ImageStorage`, `media.ImageType`,
`media.ImageService`, `media.StorageProperties` as a validated `@ConfigurationProperties` record), since only the
catalog stores images. The controller is `AdminUploadController` under `PRODUCT_WRITE`. My call.

**Trusted URLs on save.** `ProductAdminService` checks every media URL in a create or update: a URL already on that
product (unchanged since it was saved) is kept; any other URL must be one `ImageStorage.isStored` recognises
(`<public base>/<uuid>.webp`), else 400 `IMAGE_NOT_UPLOADED`. The demo catalog has no media rows, so nothing breaks.

## Backend additions for the editor

- `GET /api/admin/products/{id}/edit` (`PRODUCT_READ_ALL`): the product in the exact shape of `ProductRequest`
  (`categoryId`, `characterId`, `status`, exact option stock, facts, box items, media with kind, URL and alt text in
  order, tags, `bundleProductIds`), so the editor loads and saves the same document. Options keep their ids in the
  response for display; saves still match options by value, as today.
- `GET /api/admin/reviews?status=PUBLISHED|HIDDEN&productId=&pageNo&pageSize` (`REVIEW_MODERATE`): newest first,
  `{id, productId, productName, authorName, rating, body, status, createdAt}`. No phone or user id in the response.
- Existing and unchanged: category and character CRUD (`CATEGORY_WRITE`), user search and ban (`USER_READ`,
  `USER_UPDATE`), review status (`REVIEW_MODERATE`), product create, update, stock, archive.

## Frontend (`ecommerce_web`, branch restarted from `feat/storefront-v2-base`)

- **Products list** gets "New product" and "Edit" per row (the insights table stays the list).
- **Product editor** `/admin/products/new`, `/admin/products/:id`. One page, sections in this order: basics (name,
  slug with a suggestion from the name, category, character, maker, summary, status), price and pre-order (price,
  compare-at, pre-order release month and deposit), options (name of the group, rows of value, price difference,
  stock, colour swatch), images (photos, 360 frames, story image), story and description, facts, in the box, tags,
  set contents (pick products). Save sends the whole document; the backend's field messages show beside each field.
  Leaving with unsaved changes asks first.
- **Images:** a file picker that takes many files; each uploads on its own (3 at a time) with a progress state, a
  failed one shows the backend's message and can be retried; thumbnails can be reordered by drag and with
  move-up/move-down buttons (keyboard), and removed. 360 frames are ordered by file name on pick, so a turntable
  export (`frame-01.jpg` …) lands in order.
- **Categories and characters:** one screen each: table, inline create and edit form, delete with confirmation.
- **Users:** search by name, phone or email; ban and unban with confirmation (the backend refuses banning yourself).
- **Reviews:** filter by status; hide and publish; each shows the product, rating, text and date.
- Navigation: Dashboard, Products, Orders, Catalog (categories and characters), Users, Reviews.

## Configuration (`.env.example`)

`S3_BUCKET`, `S3_REGION`, `S3_ENDPOINT` (optional), `S3_ACCESS_KEY_ID`, `S3_SECRET_ACCESS_KEY`,
`UPLOADS_PUBLIC_BASE_URL`, `STORAGE_DIR`. Secrets have no defaults. The storefront sets no Content-Security-Policy today (`vercel.json`), so images from
`UPLOADS_PUBLIC_BASE_URL` load without a change; a CSP added later must allow that host in `img-src`.

## Security

- Upload is staff only (`PRODUCT_WRITE`), rate limited per user (60 a minute) to bound bucket spend if a staff
  token leaks. My call.
- Names are generated; nothing from the upload (name, content type) is used.
- Only JDK ImageIO decodes untrusted JPEG and PNG; WebP is never decoded; dimension limits before decoding.
- Product media must come from our store (above). Alt text stays plain text.
- `docs/SECURITY.md` S6 is replaced by these controls.

## Tests

- Backend unit: `ImageType.detect` (each signature, a PNG renamed `.jpg`, an SVG, a truncated file); WebP header
  dimensions (VP8, VP8L, VP8X); size and dimension limits; JPEG and PNG come out as WebP with the longest side at
  most 2 400 px; storage selection by property; `isStored`; the trusted-URL rule on create and update (kept old URL,
  new uploaded URL, foreign URL → 400); the edit view round-trips through `ProductRequest` without changing stock;
  review list scoping and no personal fields.
- `scripts/smoke_test.py`: upload a PNG (local storage), get a `.webp` URL that serves `image/webp`; a text file →
  400; a customer → 403; create a product with that image; a foreign URL → 400; edit view → save → stock unchanged;
  review list.
- Frontend: editor load and save (the sent body equals the loaded document after an edit of one field), field
  errors from a 400, multi-file upload with one failure and retry, reorder by keyboard, unsaved-changes prompt;
  categories, users and reviews screens. Playwright: create a product with an uploaded photo and see it in the shop.

## Steps (one PR each, stacked)

1. Backend: image pipeline, storage, upload endpoint, trusted-URL rule, config (roadmap 3.4).
2. Backend: product edit view, admin review list.
3. Frontend: product editor with image upload.
4. Frontend: categories and characters.
5. Frontend: users and reviews; Playwright; docs.

## Left to the builder

Class and component names, exact layout of the editor sections, the drag library question (native HTML drag and
drop, no new dependency, unless it proves unusable), copy.

## Calls to check (mine, not yet confirmed)

- WebP on the server with `webp-imageio`, quality 0.82, longest side 2 400 px, limits 8 000 px / 40 MP.
- WebP uploads stored without re-encoding.
- Upload code in the catalog module; 60 uploads a minute per staff user.
- URLs already on a product stay valid; new ones must be uploaded here.

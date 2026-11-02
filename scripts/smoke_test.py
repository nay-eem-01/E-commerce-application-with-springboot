"""
End-to-end smoke test against a freshly started development instance.

Usage (fresh database, development profile, INITIAL_ADMIN_EMAIL=admin@senpai.local and an
empty INITIAL_ADMIN_PASSWORD so a password is generated):

    java -jar ecommerce-api/target/ecommerce-api-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
    grep "Created admin" app.log | sed 's/.*password: \([^ ]*\).*/\1/' > adminpw.txt
    python3 scripts/smoke_test.py app.log adminpw.txt

It reads sign-in codes from the development SMS log, so it only works in development.
Covers storefront, quote, guest and member checkout, idempotency, stock, auth and token
rotation, ownership (404/403), the admin flow, the order state machine and reviews.
"""
import json, re, sys, time, uuid, urllib.request, urllib.error

BASE = "http://localhost:8080"
LOG = sys.argv[1]
ADMIN_PW = open(sys.argv[2]).read().strip()
fails = []


def call(method, path, body=None, token=None, headers=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    for k, v in (headers or {}).items():
        req.add_header(k, v)
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, json.loads(r.read() or b"null")
    except urllib.error.HTTPError as e:
        raw = e.read()
        return e.code, json.loads(raw) if raw else None


class _NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        return None


def get_no_redirect(url):
    """GET without following redirects; returns (status, Location)."""
    opener = urllib.request.build_opener(_NoRedirect)
    try:
        r = opener.open(url)
        return r.status, r.headers.get("Location")
    except urllib.error.HTTPError as e:
        return e.code, e.headers.get("Location")


def check(name, cond, info=""):
    print(("PASS " if cond else "FAIL ") + name + ("" if cond else f"  -> {info}"))
    if not cond:
        fails.append(name)


def otp_code(phone_local):
    time.sleep(0.3)
    codes = re.findall(r"\[DEV SMS - not sent\] to \+88" + phone_local + r": (\d{6})", open(LOG).read())
    return codes[-1]


# --- storefront ---
s, r = call("GET", "/api/categories")
check("categories public", s == 200 and len(r["payload"]) == 4, r)
s, r = call("GET", "/api/products?category=figures")
check("list by category", s == 200 and r["payload"]["totalElements"] == 3, r)
s, r = call("GET", "/api/products?q=wig")
check("search", s == 200 and r["payload"]["content"][0]["slug"] == "silver-wig", r)
s, r = call("GET", "/api/products?inStock=true&maxPrice=1000&sortBy=price&ascOrDesc=asc")
check("filter + sort", s == 200 and all(p["price"] <= 1000 for p in r["payload"]["content"]), r)
s, r = call("GET", "/api/products?sortBy=password")
check("unknown sort is 400", s == 400, (s, r))
s, r = call("GET", "/api/products?pageSize=5000")
check("pageSize clamped to 100", s == 200 and r["payload"]["pageSize"] == 100, r)
s, r = call("GET", "/api/products/kitsune-set")
kitsune = r["payload"]
check("product detail", s == 200 and kitsune["optionName"] == "Size" and len(kitsune["facts"]) == 4, r)
xl = [o for o in kitsune["options"] if o["value"] == "XL"][0]
check("sold-out option flagged", xl["stockState"] == "SOLD_OUT", xl)
s, r = call("GET", "/api/products/does-not-exist")
check("unknown product 404", s == 404, s)
s, r = call("GET", "/api/products/ronin-swordsman/related")
check("related", s == 200 and len(r["payload"]) == 4, r)
s, r = call("GET", "/api/checkout/delivery-options")
check("delivery options (dev: COD + fake online)", s == 200 and r["payload"]["paymentMethods"] == ["COD", "BKASH", "CARD"], r)

# --- quote ---
s, r = call("GET", "/api/products/silver-wig"); wig = r["payload"]
m = [o for o in kitsune["options"] if o["value"] == "M"][0]
s, r = call("POST", "/api/checkout/quote", {"items": [{"productId": wig["id"], "quantity": 1}], "district": "Sylhet"})
check("quote outside Dhaka fee 130", s == 200 and r["payload"]["deliveryFee"] == 130 and r["payload"]["total"] == 1480, r)
s, r = call("POST", "/api/checkout/quote", {"items": [{"productId": kitsune["id"], "optionId": m["id"], "quantity": 1}]})
check("quote over threshold is free", s == 200 and r["payload"]["deliveryFee"] == 0, r)
s, r = call("POST", "/api/checkout/quote", {"items": [{"productId": kitsune["id"], "quantity": 1}]})
check("missing option is 400", s == 400, (s, r))
s, r = call("POST", "/api/checkout/quote", {"items": [{"productId": wig["id"], "quantity": 1}], "district": "Atlantis"})
check("unknown district 400", s == 400, (s, r))

# --- guest checkout ---
order_body = {"items": [{"productId": wig["id"], "quantity": 1}], "fullName": "Rafi Islam", "phone": "01712345678",
              "district": "Dhaka", "area": "Dhanmondi", "addressLine": "House 12, Road 5", "paymentMethod": "COD"}
s, r = call("POST", "/api/orders", order_body)
check("order without Idempotency-Key 400", s == 400, (s, r))
key = str(uuid.uuid4())
s, r = call("POST", "/api/orders", order_body, headers={"Idempotency-Key": key})
check("guest order placed", s == 201 and r["payload"]["total"] == 1420 and r["payload"]["status"] == "PLACED", r)
guest_no = r["payload"]["orderNumber"]
s, r2 = call("POST", "/api/orders", order_body, headers={"Idempotency-Key": key})
check("retry same key returns same order", s == 201 and r2["payload"]["orderNumber"] == guest_no, r2)
s, r = call("GET", "/api/products/silver-wig")
check("stock decremented once (3 -> 2)", r["payload"]["stockLeft"] == 2, r["payload"]["stockLeft"])
s, r = call("POST", "/api/orders", dict(order_body, paymentMethod="NAGAD"), headers={"Idempotency-Key": str(uuid.uuid4())})
check("disabled payment method 400", s == 400, (s, r))

# --- online payment through the fake provider (development only) ---
s, r = call("GET", "/api/products/mecha-shaker"); shaker = r["payload"]
online_body = dict(order_body, items=[{"productId": shaker["id"], "quantity": 1}], paymentMethod="BKASH")
s, r = call("POST", "/api/orders", online_body, headers={"Idempotency-Key": str(uuid.uuid4())})
pay = r["payload"]["paymentAction"] if s == 201 else None
check("online order returns a redirect", s == 201 and pay["type"] == "REDIRECT" and "/api/payments/fake/callback" in pay["url"]
      and r["payload"]["paymentStatus"] == "PENDING" and r["payload"]["amountDueOnDelivery"] == 0, r)
online_no = r["payload"]["orderNumber"]
st, loc = get_no_redirect(pay["url"])
check("callback verifies and sends the shopper to the storefront", st == 303 and loc.startswith("http://localhost:5173/checkout/result")
      and f"order={online_no}" in loc and "payment=PAID" in loc, (st, loc))
st2, loc2 = get_no_redirect(pay["url"])
check("repeated callback is idempotent", st2 == 303 and "payment=PAID" in loc2, (st2, loc2))
st, loc = get_no_redirect(BASE + "/api/payments/fake/callback?ref=PAY-NOPE&outcome=success")
check("unknown payment reference", st == 303 and "payment=UNKNOWN" in loc, (st, loc))
st, loc = get_no_redirect(BASE + "/api/payments/paypal/callback?x=1")
check("unknown provider", st == 303 and "payment=UNKNOWN" in loc, (st, loc))

s, r = call("POST", "/api/orders", dict(online_body, paymentMethod="CARD"), headers={"Idempotency-Key": str(uuid.uuid4())})
card_no = r["payload"]["orderNumber"]
st, loc = get_no_redirect(r["payload"]["paymentAction"]["url"].replace("outcome=success", "outcome=fail"))
check("failed card payment", st == 303 and "payment=FAILED" in loc, (st, loc))
s, r = call("POST", "/api/orders/pay", {"orderNumber": card_no, "phone": "01811111111"})
check("retry with the wrong phone 404", s == 404, s)
s, r = call("POST", "/api/orders/pay", {"orderNumber": card_no, "phone": "01712345678"})
check("retry payment gives a new redirect", s == 200 and r["payload"]["type"] == "REDIRECT", r)
retry_url = r["payload"]["url"]
s, r = call("POST", "/api/orders/pay", {"orderNumber": online_no, "phone": "01712345678"})
check("a paid order cannot be paid again", s == 409, s)
s, r = call("POST", "/api/orders", dict(order_body, items=[{"productId": wig["id"], "quantity": 5}]), headers={"Idempotency-Key": str(uuid.uuid4())})
check("more than stock is 409", s == 409, (s, r))
s, r = call("POST", "/api/orders/track", {"orderNumber": guest_no, "phone": "01712345678"})
check("guest tracking", s == 200 and "addressLine" not in r["payload"] and r["payload"]["status"] == "PLACED", r)
s, r = call("POST", "/api/orders/track", {"orderNumber": guest_no, "phone": "01811111111"})
check("tracking wrong phone 404", s == 404, s)

# --- auth ---
s, r = call("GET", "/api/users/me")
check("me without token 401", s == 401 and r["message"], (s, r))
s, r = call("GET", "/api/users/me", token="not.a.jwt")
check("garbage token 401 not 500", s == 401, s)
s, r = call("POST", "/api/auth/otp/request", {"phone": "12345"})
check("bad phone 400", s == 400, (s, r))
s, r = call("POST", "/api/auth/otp/request", {"phone": "01911111111"})
check("otp request", s == 200, r)
s, r = call("POST", "/api/auth/otp/request", {"phone": "01911111111"})
check("otp resend cooldown 429", s == 429, s)
s, r = call("POST", "/api/auth/otp/verify", {"phone": "01911111111", "code": "000000"})
check("wrong code 400", s == 400, s)
s, r = call("POST", "/api/auth/otp/verify", {"phone": "01911111111", "code": otp_code("01911111111"), "name": "Tasnim Rahman"})
check("otp verify signs in", s == 200 and r["payload"]["user"]["phone"] == "01911111111", r)
tok, refresh = r["payload"]["accessToken"], r["payload"]["refreshToken"]
s, r = call("GET", "/api/users/me", token=tok)
check("me", s == 200 and r["payload"]["name"] == "Tasnim Rahman" and "passwordHash" not in json.dumps(r), r)

# refresh rotation + reuse detection
s, r = call("POST", "/api/auth/refresh-token", {"refreshToken": refresh})
check("refresh rotates", s == 200 and r["payload"]["refreshToken"] != refresh, r)
tok2, refresh2 = r["payload"]["accessToken"], r["payload"]["refreshToken"]
s, r = call("POST", "/api/auth/refresh-token", {"refreshToken": refresh})
check("reused refresh token 401", s == 401, s)
s, r = call("GET", "/api/users/me", token=tok2)
check("reuse revoked the whole session", s == 401, s)

s, r = call("POST", "/api/auth/otp/request", {"phone": "01911111111"})
time.sleep(61) if s == 429 else None
if s == 429:
    s, r = call("POST", "/api/auth/otp/request", {"phone": "01911111111"})
s, r = call("POST", "/api/auth/otp/verify", {"phone": "01911111111", "code": otp_code("01911111111")})
tok = r["payload"]["accessToken"]
check("sign in again", s == 200, r)

# --- cart + wishlist ---
s, r = call("PUT", "/api/cart/items", {"productId": kitsune["id"], "optionId": m["id"], "quantity": 2}, token=tok)
check("cart set line", s == 200 and r["payload"]["subtotal"] == 8400, r)
s, r = call("POST", "/api/cart/merge", {"items": [{"productId": wig["id"], "quantity": 1}, {"productId": 999999, "quantity": 1}]}, token=tok)
check("merge skips unknown products", s == 200 and len(r["payload"]["items"]) == 2, r)
s, r = call("PUT", f"/api/wishlist/{wig['id']}", token=tok)
s, r = call("GET", "/api/wishlist", token=tok)
check("wishlist", s == 200 and r["payload"][0]["slug"] == "silver-wig", r)

# --- member checkout clears bought lines and saves address ---
s, r = call("POST", "/api/orders", {"items": [{"productId": kitsune["id"], "optionId": m["id"], "quantity": 2}],
                                     "fullName": "Tasnim Rahman", "phone": "01911111111", "district": "Gazipur",
                                     "area": "Tongi", "addressLine": "Road 3, House 9", "paymentMethod": "COD"},
            token=tok, headers={"Idempotency-Key": str(uuid.uuid4())})
check("member order", s == 201 and r["payload"]["deliveryFee"] == 0 and r["payload"]["amountDueOnDelivery"] == 8400, r)
member_no = r["payload"]["orderNumber"]
s, r = call("GET", "/api/cart", token=tok)
check("bought line left the cart", s == 200 and len(r["payload"]["items"]) == 1, r)
s, r = call("GET", "/api/users/me/addresses", token=tok)
check("first address saved", s == 200 and r["payload"][0]["district"] == "Gazipur" and r["payload"][0]["defaultAddress"], r)
s, r = call("GET", "/api/orders/me", token=tok)
check("my orders", s == 200 and r["payload"]["content"][0]["orderNumber"] == member_no, r)
s, r = call("GET", f"/api/orders/me/{guest_no}", token=tok)
check("someone else's order is 404", s == 404, s)
addr_id = call("GET", "/api/users/me/addresses", token=tok)[1]["payload"][0]["id"]

# second customer cannot touch first customer's address
call("POST", "/api/auth/otp/request", {"phone": "01722222222"})
s, r = call("POST", "/api/auth/otp/verify", {"phone": "01722222222", "code": otp_code("01722222222")})
other = r["payload"]["accessToken"]
s, r = call("DELETE", f"/api/users/me/addresses/{addr_id}", token=other)
check("other user's address 404", s == 404, s)
s, r = call("GET", "/api/admin/orders", token=other)
check("customer on admin endpoint 403", s == 403, s)

# review before delivery is refused
s, r = call("POST", "/api/products/kitsune-set/reviews", {"rating": 5, "body": "Great costume, fits well."}, token=tok)
check("review before delivery 403", s == 403, (s, r))

# --- admin ---
s, r = call("POST", "/api/auth/sign-in", {"email": "admin@senpai.local", "password": "wrong-Pass1!"})
check("wrong admin password 401", s == 401 and r["message"] == "Invalid email or password.", r)
s, r = call("POST", "/api/auth/sign-in", {"email": "admin@senpai.local", "password": ADMIN_PW})
check("admin sign in", s == 200 and r["payload"]["user"]["mustChangePassword"], r)
adm = r["payload"]["accessToken"]
s, r = call("GET", "/api/admin/orders", token=adm)
check("must change password first 403", s == 403, s)
s, r = call("POST", "/api/users/me/change-password", {"currentPassword": ADMIN_PW, "newPassword": "short"}, token=adm)
check("weak new password 400", s == 400, s)
s, r = call("POST", "/api/users/me/change-password", {"currentPassword": ADMIN_PW, "newPassword": "Senpai-Admin-2026!"}, token=adm)
check("password changed", s == 200, r)
s, r = call("GET", "/api/users/me", token=adm)
check("old admin token revoked", s == 401, s)
s, r = call("POST", "/api/auth/sign-in", {"email": "admin@senpai.local", "password": "Senpai-Admin-2026!"})
adm = r["payload"]["accessToken"]
s, r = call("GET", "/api/admin/orders?status=PLACED", token=adm)
check("admin order list", s == 200 and r["payload"]["totalElements"] >= 2, r)
s, r = call("PATCH", f"/api/admin/orders/{card_no}/status", {"action": "PACK"}, token=adm)
check("unpaid online order cannot be packed", s == 409, s)
st, loc = get_no_redirect(retry_url)
s, r = call("PATCH", f"/api/admin/orders/{card_no}/status", {"action": "PACK"}, token=adm)
check("after a successful retry it can be packed", "payment=PAID" in loc and s == 200 and r["payload"]["status"] == "PACKED", (loc, s, r))
s, r = call("PATCH", f"/api/admin/orders/{online_no}/status", {"action": "CANCEL"}, token=adm)
check("cancelling a paid online order refunds it", s == 200 and r["payload"]["paymentStatus"] == "REFUNDED", r)
s, r = call("PATCH", f"/api/admin/orders/{member_no}/status", {"action": "DELIVER"}, token=adm)
check("illegal transition 409", s == 409, s)
for a in ("PACK", "SHIP", "DELIVER"):
    s, r = call("PATCH", f"/api/admin/orders/{member_no}/status", {"action": a}, token=adm)
check("pack, ship, deliver", s == 200 and r["payload"]["status"] == "DELIVERED" and r["payload"]["paymentStatus"] == "PAID", r)
check("timeline has 4 steps", len(r["payload"]["timeline"]) == 4, r["payload"]["timeline"])
s, r = call("PATCH", f"/api/admin/orders/{guest_no}/status", {"action": "CANCEL", "note": "Customer called"}, token=adm)
check("cancel guest order", s == 200 and r["payload"]["status"] == "CANCELLED", r)
s, r = call("GET", "/api/products/silver-wig")
check("cancel restored stock (2 -> 3)", r["payload"]["stockLeft"] == 3, r["payload"]["stockLeft"])

# --- review after delivery ---
s, r = call("POST", "/api/products/kitsune-set/reviews", {"rating": 5, "body": "Great costume, fits well."}, token=tok)
check("verified review", s == 201 and r["payload"]["authorName"] == "Tasnim R.", r)
review_id = r["payload"]["id"]
s, r = call("POST", "/api/products/kitsune-set/reviews", {"rating": 4, "body": "Second try at a review."}, token=tok)
check("second review 409", s == 409, s)
s, r = call("PUT", f"/api/reviews/{review_id}", {"rating": 1, "body": "Trying to edit someone else's."}, token=other)
check("edit other's review 403", s == 403, s)
s, r = call("GET", "/api/products/kitsune-set/reviews")
check("review summary", s == 200 and r["payload"]["summary"]["count"] == 1 and r["payload"]["summary"]["average"] == 5.0, r)

# --- product admin ---
s, r = call("GET", "/api/admin/categories", token=adm)
cat_id = r["payload"][0]["id"]
bad = {"slug": "x-test", "categoryId": cat_id, "name": "Test", "summary": "Test product", "price": 100,
       "status": "ACTIVE", "media": [{"kind": "PHOTO", "url": "javascript:alert(1)"}]}
s, r = call("POST", "/api/admin/products", bad, token=adm)
check("javascript: URL rejected", s == 400, (s, r))
good = dict(bad, media=[{"kind": "SPIN_FRAME", "url": "https://cdn.example.com/x/01.jpg"}], stock=5)
s, r = call("POST", "/api/admin/products", good, token=adm)
check("admin creates product", s == 201 and r["payload"]["spinFrames"] == ["https://cdn.example.com/x/01.jpg"], r)

# sign out
s, r = call("POST", "/api/auth/sign-out", token=tok)
s2, r2 = call("GET", "/api/users/me", token=tok)
check("sign-out kills the token", s == 200 and s2 == 401, (s, s2))

# errors carry a correlation id and no internals
s, r = call("GET", "/api/nope", token=adm)
check("unknown endpoint 404 envelope", s == 404 and r["correlationId"] and r["success"] is False, (s, r))

print(f"\n{len(fails)} failed" if fails else "\nALL PASSED")
sys.exit(1 if fails else 0)

-- DEVELOPMENT ONLY (loaded by the development profile from db/dev-seed).
-- Demo catalog for the launch range: bottles and school bags in three characters, one set, and apparel
-- announced as coming soon. Repeatable and idempotent: existing rows are left alone. Prices, stock and
-- products are sample data. No images: the storefront draws stand-in product art until photos exist.

INSERT INTO categories (slug, name, label, blurb, sort_order, active, coming_soon, created_by) VALUES ('bottles', 'Bottles', '水', 'Steel and tritan, 500 ml to 1 L. Cold for 24 hours.', 1, TRUE, FALSE, 'SEED') ON CONFLICT (slug) DO NOTHING;
INSERT INTO categories (slug, name, label, blurb, sort_order, active, coming_soon, created_by) VALUES ('bags', 'School bags', '鞄', 'Backpacks, minis and sling bags, 5 to 22 L.', 2, TRUE, FALSE, 'SEED') ON CONFLICT (slug) DO NOTHING;
INSERT INTO categories (slug, name, label, blurb, sort_order, active, coming_soon, created_by) VALUES ('apparel', 'Apparel', '衣', 'Tees and hoodies. Coming soon.', 3, TRUE, TRUE, 'SEED') ON CONFLICT (slug) DO NOTHING;

INSERT INTO characters (slug, name, label, blurb, sort_order, active, created_by) VALUES ('kitsune', 'Kitsune', '狐', 'Fox spirit with a lantern-red mask. Comes out after dark.', 1, TRUE, 'SEED') ON CONFLICT (slug) DO NOTHING;
INSERT INTO characters (slug, name, label, blurb, sort_order, active, created_by) VALUES ('ramen-cat', 'Ramen Cat', '猫', 'The kids range: light, bright and hard to lose.', 2, TRUE, 'SEED') ON CONFLICT (slug) DO NOTHING;
INSERT INTO characters (slug, name, label, blurb, sort_order, active, created_by) VALUES ('ronin', 'Ronin', '侍', 'Black and red, for long days.', 3, TRUE, 'SEED') ON CONFLICT (slug) DO NOTHING;

INSERT INTO products (slug, category_id, character_id, name, maker, summary, description, story, story_quote, spec_line, price, compare_at_price, stock, preorder, preorder_release, deposit, sized, option_name, status, created_by, creation_date)
SELECT 'kitsune-night-bottle', c.id, (SELECT id FROM characters WHERE slug = 'kitsune'), 'Kitsune Night steel bottle, 750 ml', 'Senpai Supply', 'Double-wall steel with a falling-sakura band and our Kitsune fox. Cold for a whole school day.', 'Who it is for: students and commuters who carry water all day.

The lid seals with a quarter turn and does not leak in a bag. The print is fired on, so it does not peel.', 'Kitsune are fox spirits from folklore. Ours wears a lantern-red mask and only comes out after dark.', 'Count the tails on the band.', '750 ml, cold for 24 h', 1450, NULL, 0, FALSE, NULL, NULL, FALSE, 'Colour', 'ACTIVE', 'SEED', now() - interval '9 hours'
FROM categories c WHERE c.slug = 'bottles'
ON CONFLICT (slug) DO NOTHING;
INSERT INTO product_facts (product_id, label, fact_value, sort_order) SELECT p.id, v.l, v.v, v.o FROM products p, (VALUES ('Capacity', '750 ml', 0), ('Keeps cold', '24 hours', 1), ('Keeps hot', '12 hours', 2), ('Body', '304 steel', 3)) AS v(l, v, o) WHERE p.slug = 'kitsune-night-bottle' AND NOT EXISTS (SELECT 1 FROM product_facts f WHERE f.product_id = p.id);
INSERT INTO product_box_items (product_id, item, sort_order) SELECT p.id, v.i, v.o FROM products p, (VALUES ('Bottle', 0), ('Leak-proof lid', 1), ('Spare seal', 2)) AS v(i, o) WHERE p.slug = 'kitsune-night-bottle' AND NOT EXISTS (SELECT 1 FROM product_box_items b WHERE b.product_id = p.id);
INSERT INTO product_tags (product_id, tag_group, tag_value, sort_order) SELECT p.id, v.g, v.v, v.o FROM products p, (VALUES ('Capacity', '750 ml', 0), ('Material', 'Steel', 1)) AS v(g, v, o) WHERE p.slug = 'kitsune-night-bottle' AND NOT EXISTS (SELECT 1 FROM product_tags t WHERE t.product_id = p.id);
INSERT INTO product_options (product_id, option_value, price_delta, stock, swatch, sort_order, created_by) SELECT p.id, v.val, v.d, v.s, v.sw, v.o, 'SEED' FROM products p, (VALUES ('Petal pink', 0, 4, '#f2a7c3', 0), ('Night navy', 0, 12, '#1f2a4d', 1)) AS v(val, d, s, sw, o) WHERE p.slug = 'kitsune-night-bottle' AND NOT EXISTS (SELECT 1 FROM product_options x WHERE x.product_id = p.id);
UPDATE products p SET stock = (SELECT COALESCE(SUM(o.stock), 0) FROM product_options o WHERE o.product_id = p.id) WHERE p.slug = 'kitsune-night-bottle' AND p.created_by = 'SEED';

INSERT INTO products (slug, category_id, character_id, name, maker, summary, description, story, story_quote, spec_line, price, compare_at_price, stock, preorder, preorder_release, deposit, sized, option_name, status, created_by, creation_date)
SELECT 'ramen-cat-bottle', c.id, (SELECT id FROM characters WHERE slug = 'ramen-cat'), 'Ramen Cat kids bottle, 500 ml', 'Senpai Supply', 'A light bottle with a flip-up straw and a cat slurping ramen round the side.', 'Who it is for: school bags and small hands.

The straw locks down so it does not leak on the way to class.', 'The cat appears in three poses as you turn the bottle: blowing, slurping, full.', 'Blow, slurp, full.', '500 ml, flip straw', 890, NULL, 3, FALSE, NULL, NULL, FALSE, NULL, 'ACTIVE', 'SEED', now() - interval '8 hours'
FROM categories c WHERE c.slug = 'bottles'
ON CONFLICT (slug) DO NOTHING;
INSERT INTO product_facts (product_id, label, fact_value, sort_order) SELECT p.id, v.l, v.v, v.o FROM products p, (VALUES ('Capacity', '500 ml', 0), ('Material', 'Tritan, BPA free', 1), ('Straw', 'Silicone, flip-up', 2), ('Age', '3+', 3)) AS v(l, v, o) WHERE p.slug = 'ramen-cat-bottle' AND NOT EXISTS (SELECT 1 FROM product_facts f WHERE f.product_id = p.id);
INSERT INTO product_box_items (product_id, item, sort_order) SELECT p.id, v.i, v.o FROM products p, (VALUES ('Bottle', 0), ('Spare straw', 1)) AS v(i, o) WHERE p.slug = 'ramen-cat-bottle' AND NOT EXISTS (SELECT 1 FROM product_box_items b WHERE b.product_id = p.id);
INSERT INTO product_tags (product_id, tag_group, tag_value, sort_order) SELECT p.id, v.g, v.v, v.o FROM products p, (VALUES ('Capacity', '500 ml', 0), ('Material', 'Tritan', 1)) AS v(g, v, o) WHERE p.slug = 'ramen-cat-bottle' AND NOT EXISTS (SELECT 1 FROM product_tags t WHERE t.product_id = p.id);

INSERT INTO products (slug, category_id, character_id, name, maker, summary, description, story, story_quote, spec_line, price, compare_at_price, stock, preorder, preorder_release, deposit, sized, option_name, status, created_by, creation_date)
SELECT 'ronin-bottle', c.id, (SELECT id FROM characters WHERE slug = 'ronin'), 'Ronin black steel bottle, 1 L', 'Senpai Supply', 'A litre of steel in black with a red slash, for long days and football practice.', 'Who it is for: anyone who forgets to refill.

Powder-coated, so it grips when your hands are wet.', 'Ronin walks alone and never runs out of water.', 'One litre, no refills.', '1 L, cold for 24 h', 1650, NULL, 15, FALSE, NULL, NULL, FALSE, NULL, 'ACTIVE', 'SEED', now() - interval '7 hours'
FROM categories c WHERE c.slug = 'bottles'
ON CONFLICT (slug) DO NOTHING;
INSERT INTO product_facts (product_id, label, fact_value, sort_order) SELECT p.id, v.l, v.v, v.o FROM products p, (VALUES ('Capacity', '1 litre', 0), ('Keeps cold', '24 hours', 1), ('Keeps hot', '12 hours', 2), ('Body', '304 steel', 3)) AS v(l, v, o) WHERE p.slug = 'ronin-bottle' AND NOT EXISTS (SELECT 1 FROM product_facts f WHERE f.product_id = p.id);
INSERT INTO product_box_items (product_id, item, sort_order) SELECT p.id, v.i, v.o FROM products p, (VALUES ('Bottle', 0), ('Carry-loop lid', 1)) AS v(i, o) WHERE p.slug = 'ronin-bottle' AND NOT EXISTS (SELECT 1 FROM product_box_items b WHERE b.product_id = p.id);
INSERT INTO product_tags (product_id, tag_group, tag_value, sort_order) SELECT p.id, v.g, v.v, v.o FROM products p, (VALUES ('Capacity', '1 L', 0), ('Material', 'Steel', 1)) AS v(g, v, o) WHERE p.slug = 'ronin-bottle' AND NOT EXISTS (SELECT 1 FROM product_tags t WHERE t.product_id = p.id);

INSERT INTO products (slug, category_id, character_id, name, maker, summary, description, story, story_quote, spec_line, price, compare_at_price, stock, preorder, preorder_release, deposit, sized, option_name, status, created_by, creation_date)
SELECT 'sakura-shaker', c.id, NULL, 'Sakura tritan shaker, 600 ml', 'Senpai Supply', 'A BPA-free shaker with a mixing coil and a sakura print all the way round.', 'Who it is for: gym days and protein shakes.

Scale marks every 100 ml are moulded in, so they never wear off.', 'Petals all the way round, so it looks right from any side.', 'Shake, sip, repeat.', '600 ml, leak-proof', 950, NULL, 21, FALSE, NULL, NULL, FALSE, NULL, 'ACTIVE', 'SEED', now() - interval '6 hours'
FROM categories c WHERE c.slug = 'bottles'
ON CONFLICT (slug) DO NOTHING;
INSERT INTO product_facts (product_id, label, fact_value, sort_order) SELECT p.id, v.l, v.v, v.o FROM products p, (VALUES ('Capacity', '600 ml', 0), ('Material', 'Tritan, BPA free', 1), ('Mixer', 'Steel coil', 2), ('Dishwasher', 'Top rack', 3)) AS v(l, v, o) WHERE p.slug = 'sakura-shaker' AND NOT EXISTS (SELECT 1 FROM product_facts f WHERE f.product_id = p.id);
INSERT INTO product_box_items (product_id, item, sort_order) SELECT p.id, v.i, v.o FROM products p, (VALUES ('Shaker', 0), ('Mixing coil', 1)) AS v(i, o) WHERE p.slug = 'sakura-shaker' AND NOT EXISTS (SELECT 1 FROM product_box_items b WHERE b.product_id = p.id);
INSERT INTO product_tags (product_id, tag_group, tag_value, sort_order) SELECT p.id, v.g, v.v, v.o FROM products p, (VALUES ('Capacity', '600 ml', 0), ('Material', 'Tritan', 1)) AS v(g, v, o) WHERE p.slug = 'sakura-shaker' AND NOT EXISTS (SELECT 1 FROM product_tags t WHERE t.product_id = p.id);

INSERT INTO products (slug, category_id, character_id, name, maker, summary, description, story, story_quote, spec_line, price, compare_at_price, stock, preorder, preorder_release, deposit, sized, option_name, status, created_by, creation_date)
SELECT 'kitsune-tritan-bottle', c.id, (SELECT id FROM characters WHERE slug = 'kitsune'), 'Kitsune tritan bottle, 500 ml', 'Senpai Supply', 'The Kitsune bottle in a light kids size, in hot pink.', 'Who it is for: younger fans who want what their older sibling has.

Light enough for a nursery bag.', 'The same fox, a smaller lantern.', 'Small fox, same mask.', '500 ml, light tritan', 790, NULL, 0, TRUE, '2027-01-01'::date, 300, FALSE, NULL, 'ACTIVE', 'SEED', now() - interval '5 hours'
FROM categories c WHERE c.slug = 'bottles'
ON CONFLICT (slug) DO NOTHING;
INSERT INTO product_facts (product_id, label, fact_value, sort_order) SELECT p.id, v.l, v.v, v.o FROM products p, (VALUES ('Capacity', '500 ml', 0), ('Material', 'Tritan, BPA free', 1), ('Weight', '180 g', 2), ('Age', '6+', 3)) AS v(l, v, o) WHERE p.slug = 'kitsune-tritan-bottle' AND NOT EXISTS (SELECT 1 FROM product_facts f WHERE f.product_id = p.id);
INSERT INTO product_box_items (product_id, item, sort_order) SELECT p.id, v.i, v.o FROM products p, (VALUES ('Bottle', 0)) AS v(i, o) WHERE p.slug = 'kitsune-tritan-bottle' AND NOT EXISTS (SELECT 1 FROM product_box_items b WHERE b.product_id = p.id);
INSERT INTO product_tags (product_id, tag_group, tag_value, sort_order) SELECT p.id, v.g, v.v, v.o FROM products p, (VALUES ('Capacity', '500 ml', 0), ('Material', 'Tritan', 1)) AS v(g, v, o) WHERE p.slug = 'kitsune-tritan-bottle' AND NOT EXISTS (SELECT 1 FROM product_tags t WHERE t.product_id = p.id);

INSERT INTO products (slug, category_id, character_id, name, maker, summary, description, story, story_quote, spec_line, price, compare_at_price, stock, preorder, preorder_release, deposit, sized, option_name, status, created_by, creation_date)
SELECT 'kitsune-backpack', c.id, (SELECT id FROM characters WHERE slug = 'kitsune'), 'Kitsune school backpack, 18 L', 'Senpai Supply', 'A school backpack with the Kitsune mask on the front and a side pocket sized for our bottles.', 'Who it is for: class 5 to class 10.

Padded straps, a chest clip and a side pocket that holds the 750 ml bottle.', 'Designed around the walk to school: everything you reach for is on the outside.', 'Fox in front, bottle on the side.', '18 L, fits A4 and a 14 in laptop', 2950, 3300, 0, FALSE, NULL, NULL, FALSE, 'Colour', 'ACTIVE', 'SEED', now() - interval '4 hours'
FROM categories c WHERE c.slug = 'bags'
ON CONFLICT (slug) DO NOTHING;
INSERT INTO product_facts (product_id, label, fact_value, sort_order) SELECT p.id, v.l, v.v, v.o FROM products p, (VALUES ('Capacity', '18 litres', 0), ('Laptop', 'Up to 14 inch', 1), ('Fabric', 'Water-resistant nylon', 2), ('Weight', '620 g', 3)) AS v(l, v, o) WHERE p.slug = 'kitsune-backpack' AND NOT EXISTS (SELECT 1 FROM product_facts f WHERE f.product_id = p.id);
INSERT INTO product_box_items (product_id, item, sort_order) SELECT p.id, v.i, v.o FROM products p, (VALUES ('Backpack', 0), ('Rain cover', 1)) AS v(i, o) WHERE p.slug = 'kitsune-backpack' AND NOT EXISTS (SELECT 1 FROM product_box_items b WHERE b.product_id = p.id);
INSERT INTO product_tags (product_id, tag_group, tag_value, sort_order) SELECT p.id, v.g, v.v, v.o FROM products p, (VALUES ('Capacity', '18 L', 0), ('Material', 'Nylon', 1)) AS v(g, v, o) WHERE p.slug = 'kitsune-backpack' AND NOT EXISTS (SELECT 1 FROM product_tags t WHERE t.product_id = p.id);
INSERT INTO product_options (product_id, option_value, price_delta, stock, swatch, sort_order, created_by) SELECT p.id, v.val, v.d, v.s, v.sw, v.o, 'SEED' FROM products p, (VALUES ('Petal pink', 0, 6, '#f2a7c3', 0), ('Night black', 0, 0, '#1b1b26', 1)) AS v(val, d, s, sw, o) WHERE p.slug = 'kitsune-backpack' AND NOT EXISTS (SELECT 1 FROM product_options x WHERE x.product_id = p.id);
UPDATE products p SET stock = (SELECT COALESCE(SUM(o.stock), 0) FROM product_options o WHERE o.product_id = p.id) WHERE p.slug = 'kitsune-backpack' AND p.created_by = 'SEED';

INSERT INTO products (slug, category_id, character_id, name, maker, summary, description, story, story_quote, spec_line, price, compare_at_price, stock, preorder, preorder_release, deposit, sized, option_name, status, created_by, creation_date)
SELECT 'ita-backpack', c.id, NULL, 'Ita backpack with clear window, 22 L', 'Senpai Supply', 'A backpack with a clear front window to show off your pins and badges.', 'Who it is for: fans who collect pins and badges.

The window panel unzips so you can change the display. Behind it, a padded laptop sleeve sits against your back.', 'Ita bags started at Japanese conventions, where fans covered bags with badges of one character. The word comes from itai: painfully devoted.', 'Painfully devoted, in a good way.', '22 L, fits a 15 in laptop', 3600, NULL, 0, FALSE, NULL, NULL, FALSE, 'Colour', 'ACTIVE', 'SEED', now() - interval '3 hours'
FROM categories c WHERE c.slug = 'bags'
ON CONFLICT (slug) DO NOTHING;
INSERT INTO product_facts (product_id, label, fact_value, sort_order) SELECT p.id, v.l, v.v, v.o FROM products p, (VALUES ('Capacity', '22 litres', 0), ('Laptop', 'Up to 15 inch', 1), ('Window', 'Removable clear panel', 2), ('Fabric', 'Water-resistant nylon', 3)) AS v(l, v, o) WHERE p.slug = 'ita-backpack' AND NOT EXISTS (SELECT 1 FROM product_facts f WHERE f.product_id = p.id);
INSERT INTO product_box_items (product_id, item, sort_order) SELECT p.id, v.i, v.o FROM products p, (VALUES ('Backpack', 0), ('Pin board insert', 1)) AS v(i, o) WHERE p.slug = 'ita-backpack' AND NOT EXISTS (SELECT 1 FROM product_box_items b WHERE b.product_id = p.id);
INSERT INTO product_tags (product_id, tag_group, tag_value, sort_order) SELECT p.id, v.g, v.v, v.o FROM products p, (VALUES ('Capacity', '22 L', 0), ('Material', 'Nylon', 1)) AS v(g, v, o) WHERE p.slug = 'ita-backpack' AND NOT EXISTS (SELECT 1 FROM product_tags t WHERE t.product_id = p.id);
INSERT INTO product_options (product_id, option_value, price_delta, stock, swatch, sort_order, created_by) SELECT p.id, v.val, v.d, v.s, v.sw, v.o, 'SEED' FROM products p, (VALUES ('Black', 0, 6, '#1b1b26', 0), ('Sakura pink', 0, 5, '#f2a7c3', 1)) AS v(val, d, s, sw, o) WHERE p.slug = 'ita-backpack' AND NOT EXISTS (SELECT 1 FROM product_options x WHERE x.product_id = p.id);
UPDATE products p SET stock = (SELECT COALESCE(SUM(o.stock), 0) FROM product_options o WHERE o.product_id = p.id) WHERE p.slug = 'ita-backpack' AND p.created_by = 'SEED';

INSERT INTO products (slug, category_id, character_id, name, maker, summary, description, story, story_quote, spec_line, price, compare_at_price, stock, preorder, preorder_release, deposit, sized, option_name, status, created_by, creation_date)
SELECT 'ramen-cat-mini-backpack', c.id, (SELECT id FROM characters WHERE slug = 'ramen-cat'), 'Ramen Cat mini backpack, 10 L', 'Senpai Supply', 'A small canvas backpack for nursery to class 4, with the ramen cat on the pocket.', 'Who it is for: the youngest in the house.

A name tag inside the flap and reflective strips on the straps.', 'The cat keeps a spare noodle in the front pocket.', 'Small bag, big lunch.', '10 L, nursery to class 4', 1950, NULL, 9, FALSE, NULL, NULL, FALSE, NULL, 'ACTIVE', 'SEED', now() - interval '2 hours'
FROM categories c WHERE c.slug = 'bags'
ON CONFLICT (slug) DO NOTHING;
INSERT INTO product_facts (product_id, label, fact_value, sort_order) SELECT p.id, v.l, v.v, v.o FROM products p, (VALUES ('Capacity', '10 litres', 0), ('Fabric', 'Cotton canvas', 1), ('Weight', '380 g', 2), ('Age', '4+', 3)) AS v(l, v, o) WHERE p.slug = 'ramen-cat-mini-backpack' AND NOT EXISTS (SELECT 1 FROM product_facts f WHERE f.product_id = p.id);
INSERT INTO product_box_items (product_id, item, sort_order) SELECT p.id, v.i, v.o FROM products p, (VALUES ('Backpack', 0)) AS v(i, o) WHERE p.slug = 'ramen-cat-mini-backpack' AND NOT EXISTS (SELECT 1 FROM product_box_items b WHERE b.product_id = p.id);
INSERT INTO product_tags (product_id, tag_group, tag_value, sort_order) SELECT p.id, v.g, v.v, v.o FROM products p, (VALUES ('Capacity', '10 L', 0), ('Material', 'Canvas', 1)) AS v(g, v, o) WHERE p.slug = 'ramen-cat-mini-backpack' AND NOT EXISTS (SELECT 1 FROM product_tags t WHERE t.product_id = p.id);

INSERT INTO products (slug, category_id, character_id, name, maker, summary, description, story, story_quote, spec_line, price, compare_at_price, stock, preorder, preorder_release, deposit, sized, option_name, status, created_by, creation_date)
SELECT 'ronin-sling', c.id, (SELECT id FROM characters WHERE slug = 'ronin'), 'Ronin sling bag, 5 L', 'Senpai Supply', 'A slim cross-body bag with a hidden phone pocket against your back.', 'Who it is for: crowded buses and markets.

The phone pocket opens only from the side that faces you.', 'Wrapped like a sword handle, with the same diagonal stitching.', 'Wrapped like a hilt.', '5 L, cross-body', 1650, NULL, 0, TRUE, '2026-12-01'::date, 500, FALSE, NULL, 'ACTIVE', 'SEED', now() - interval '1 hour'
FROM categories c WHERE c.slug = 'bags'
ON CONFLICT (slug) DO NOTHING;
INSERT INTO product_facts (product_id, label, fact_value, sort_order) SELECT p.id, v.l, v.v, v.o FROM products p, (VALUES ('Capacity', '5 litres', 0), ('Strap', 'Adjustable, 70 to 130 cm', 1), ('Pockets', '3, one hidden', 2), ('Fabric', 'Ripstop nylon', 3)) AS v(l, v, o) WHERE p.slug = 'ronin-sling' AND NOT EXISTS (SELECT 1 FROM product_facts f WHERE f.product_id = p.id);
INSERT INTO product_box_items (product_id, item, sort_order) SELECT p.id, v.i, v.o FROM products p, (VALUES ('Sling bag', 0)) AS v(i, o) WHERE p.slug = 'ronin-sling' AND NOT EXISTS (SELECT 1 FROM product_box_items b WHERE b.product_id = p.id);
INSERT INTO product_tags (product_id, tag_group, tag_value, sort_order) SELECT p.id, v.g, v.v, v.o FROM products p, (VALUES ('Capacity', '5 L', 0), ('Material', 'Ripstop nylon', 1)) AS v(g, v, o) WHERE p.slug = 'ronin-sling' AND NOT EXISTS (SELECT 1 FROM product_tags t WHERE t.product_id = p.id);

INSERT INTO products (slug, category_id, character_id, name, maker, summary, description, story, story_quote, spec_line, price, compare_at_price, stock, preorder, preorder_release, deposit, sized, option_name, status, created_by, creation_date)
SELECT 'kitsune-school-run-set', c.id, (SELECT id FROM characters WHERE slug = 'kitsune'), 'Kitsune school-run set', 'Senpai Supply', 'The Kitsune 18 L backpack with the 750 ml steel bottle in its side pocket. One parcel, one delivery fee.', 'Who it is for: the first day of term.

Both pieces in Petal pink. The bottle rides in the side pocket.', 'Made to be bought together: the pocket was cut around the bottle.', 'Backpack and bottle, matched.', 'Backpack + 750 ml bottle', 3990, 4400, 5, FALSE, NULL, NULL, FALSE, NULL, 'ACTIVE', 'SEED', now() - interval '30 minutes'
FROM categories c WHERE c.slug = 'bags'
ON CONFLICT (slug) DO NOTHING;
INSERT INTO product_facts (product_id, label, fact_value, sort_order) SELECT p.id, v.l, v.v, v.o FROM products p, (VALUES ('Holds', 'Backpack + bottle', 0), ('Backpack', '18 litres', 1), ('Bottle', '750 ml steel', 2), ('Colour', 'Petal pink', 3)) AS v(l, v, o) WHERE p.slug = 'kitsune-school-run-set' AND NOT EXISTS (SELECT 1 FROM product_facts f WHERE f.product_id = p.id);
INSERT INTO product_box_items (product_id, item, sort_order) SELECT p.id, v.i, v.o FROM products p, (VALUES ('Kitsune school backpack, 18 L', 0), ('Kitsune Night steel bottle, 750 ml', 1)) AS v(i, o) WHERE p.slug = 'kitsune-school-run-set' AND NOT EXISTS (SELECT 1 FROM product_box_items b WHERE b.product_id = p.id);
INSERT INTO product_bundle_items (bundle_id, component_id, quantity, sort_order) SELECT b.id, c.id, 1, v.o FROM products b, products c, (VALUES ('kitsune-backpack', 0), ('kitsune-night-bottle', 1)) AS v(slug, o) WHERE b.slug = 'kitsune-school-run-set' AND c.slug = v.slug AND NOT EXISTS (SELECT 1 FROM product_bundle_items x WHERE x.bundle_id = b.id);

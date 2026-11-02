-- DEVELOPMENT ONLY (loaded by the development profile from db/dev-seed).
-- Demo catalog matching the storefront prototype. Repeatable and idempotent: existing rows are left alone.
-- Prices, stock and products are sample data. No images: the storefront shows labelled photo slots.

INSERT INTO categories (slug, name, label, blurb, sort_order, active, created_by) VALUES ('figures', 'Figures', '像', 'Scale figures, chibi figures and model kits', 1, TRUE, 'SEED') ON CONFLICT (slug) DO NOTHING;
INSERT INTO categories (slug, name, label, blurb, sort_order, active, created_by) VALUES ('costumes', 'Costumes', '衣', 'Full cosplay sets, wigs and props', 2, TRUE, 'SEED') ON CONFLICT (slug) DO NOTHING;
INSERT INTO categories (slug, name, label, blurb, sort_order, active, created_by) VALUES ('bottles', 'Bottles', '水', 'Steel and tritan, 500 to 1000 ml', 3, TRUE, 'SEED') ON CONFLICT (slug) DO NOTHING;
INSERT INTO categories (slug, name, label, blurb, sort_order, active, created_by) VALUES ('bags', 'Bags', '鞄', 'Backpacks, totes and sling bags', 4, TRUE, 'SEED') ON CONFLICT (slug) DO NOTHING;

INSERT INTO products (slug, category_id, name, maker, summary, description, story, story_quote, spec_line, price, compare_at_price, stock, preorder, preorder_release, deposit, sized, option_name, status, created_by, creation_date)
SELECT 'ronin-swordsman', c.id, 'Ronin Swordsman 1/7 scale figure', 'Kurogane Studio', 'A wandering swordsman mid-draw, coat caught in the wind, on a stone-step base.', 'Who it is for: collectors who want one centrepiece figure for the shelf.

The coat is a separate soft-PVC part, so it flares without looking stiff. Swap the drawn sword and the sheathed one in under a minute; both slot into the right hand by a hidden peg.

Paint is hand-finished: look for the worn edge on the scabbard and the frayed hem of the scarf.', 'The sculptor started from a single sketch of a swordsman walking into rain, and built the pose around the moment before the draw.

Every coat fold was sculpted at twice the size, then scaled down, which is why the cloth still reads at 24 cm.', 'We wanted the figure to look like it was about to move.', '24 cm, PVC and ABS', 6800, 7500, 4, FALSE, NULL, NULL, FALSE, 'Edition', 'ACTIVE', 'SEED', now() - interval '12 hours'
FROM categories c WHERE c.slug = 'figures'
ON CONFLICT (slug) DO NOTHING;
INSERT INTO product_facts (product_id, label, fact_value, sort_order) SELECT p.id, v.l, v.v, v.o FROM products p, (VALUES ('Height', '24 cm with base', 0), ('Scale', '1/7', 1), ('Material', 'PVC, ABS', 2), ('Weight', '620 g boxed', 3)) AS v(l, v, o) WHERE p.slug = 'ronin-swordsman' AND NOT EXISTS (SELECT 1 FROM product_facts f WHERE f.product_id = p.id);
INSERT INTO product_box_items (product_id, item, sort_order) SELECT p.id, v.i, v.o FROM products p, (VALUES ('Figure', 0), ('Two swords, sheathed and drawn', 1), ('Stone-step base', 2), ('Alternate face (Deluxe only)', 3)) AS v(i, o) WHERE p.slug = 'ronin-swordsman' AND NOT EXISTS (SELECT 1 FROM product_box_items b WHERE b.product_id = p.id);
INSERT INTO product_options (product_id, option_value, price_delta, stock, swatch, sort_order, created_by) SELECT p.id, v.val, v.d, v.s, v.sw, v.o, 'SEED' FROM products p, (VALUES ('Standard', 0, 3, NULL, 0), ('Deluxe, with alt face', 1200, 1, NULL, 1)) AS v(val, d, s, sw, o) WHERE p.slug = 'ronin-swordsman' AND NOT EXISTS (SELECT 1 FROM product_options x WHERE x.product_id = p.id);

INSERT INTO products (slug, category_id, name, maker, summary, description, story, story_quote, spec_line, price, compare_at_price, stock, preorder, preorder_release, deposit, sized, option_name, status, created_by, creation_date)
SELECT 'chibi-blind-box', c.id, 'Chibi heroes blind box', 'Pocket Parade', 'One random chibi hero per box. Six designs, one rare gold variant.', 'Who it is for: gift buyers and anyone who likes the surprise.

The full set guarantees all six designs with no duplicates. The rare gold variant only comes in single boxes.', 'Each hero was drawn as a full-size character first, then squashed to chibi proportions, two and a half heads tall.', 'Small enough for a desk, loud enough to notice.', '8 cm, one of six', 690, NULL, 60, FALSE, NULL, NULL, FALSE, 'Pack', 'ACTIVE', 'SEED', now() - interval '11 hours'
FROM categories c WHERE c.slug = 'figures'
ON CONFLICT (slug) DO NOTHING;
INSERT INTO product_facts (product_id, label, fact_value, sort_order) SELECT p.id, v.l, v.v, v.o FROM products p, (VALUES ('Height', '8 cm', 0), ('Designs', '6 plus 1 rare', 1), ('Material', 'PVC', 2), ('Age', '8+', 3)) AS v(l, v, o) WHERE p.slug = 'chibi-blind-box' AND NOT EXISTS (SELECT 1 FROM product_facts f WHERE f.product_id = p.id);
INSERT INTO product_box_items (product_id, item, sort_order) SELECT p.id, v.i, v.o FROM products p, (VALUES ('One sealed figure', 0), ('Display card', 1)) AS v(i, o) WHERE p.slug = 'chibi-blind-box' AND NOT EXISTS (SELECT 1 FROM product_box_items b WHERE b.product_id = p.id);
INSERT INTO product_options (product_id, option_value, price_delta, stock, swatch, sort_order, created_by) SELECT p.id, v.val, v.d, v.s, v.sw, v.o, 'SEED' FROM products p, (VALUES ('Single box', 0, 50, NULL, 0), ('Full set of 6', 3210, 10, NULL, 1)) AS v(val, d, s, sw, o) WHERE p.slug = 'chibi-blind-box' AND NOT EXISTS (SELECT 1 FROM product_options x WHERE x.product_id = p.id);

INSERT INTO products (slug, category_id, name, maker, summary, description, story, story_quote, spec_line, price, compare_at_price, stock, preorder, preorder_release, deposit, sized, option_name, status, created_by, creation_date)
SELECT 'mecha-unit-07', c.id, 'Mecha Unit-07 model kit, 1/100', 'Gearframe', 'A snap-fit mecha kit with 60 points of articulation. No glue, no paint needed.', 'Who it is for: first-time builders. The parts are colour-moulded, so it looks finished straight off the runner.

Joints are pre-tensioned, so the kit holds a pose without loosening.', 'Unit-07 is designed as the scout of its squad: lighter armour, longer legs, and a sensor head that tilts on two axes.', 'Built to be built, not glued.', '18 cm, snap-fit, no glue', 2950, NULL, 40, TRUE, '2027-01-01', 1000, FALSE, NULL, 'ACTIVE', 'SEED', now() - interval '10 hours'
FROM categories c WHERE c.slug = 'figures'
ON CONFLICT (slug) DO NOTHING;
INSERT INTO product_facts (product_id, label, fact_value, sort_order) SELECT p.id, v.l, v.v, v.o FROM products p, (VALUES ('Height', '18 cm built', 0), ('Scale', '1/100', 1), ('Parts', '212', 2), ('Build time', 'About 4 hours', 3)) AS v(l, v, o) WHERE p.slug = 'mecha-unit-07' AND NOT EXISTS (SELECT 1 FROM product_facts f WHERE f.product_id = p.id);
INSERT INTO product_box_items (product_id, item, sort_order) SELECT p.id, v.i, v.o FROM products p, (VALUES ('Six runners of parts', 0), ('Decal sheet', 1), ('Instruction booklet', 2)) AS v(i, o) WHERE p.slug = 'mecha-unit-07' AND NOT EXISTS (SELECT 1 FROM product_box_items b WHERE b.product_id = p.id);

INSERT INTO products (slug, category_id, name, maker, summary, description, story, story_quote, spec_line, price, compare_at_price, stock, preorder, preorder_release, deposit, sized, option_name, status, created_by, creation_date)
SELECT 'kitsune-set', c.id, 'Kitsune mask cosplay set', 'Senpai Supply atelier', 'A fox-spirit look in three pieces: lacquered mask, printed haori, wide obi.', 'Who it is for: con-goers who want a full look in one order.

The haori is cut loose, so it fits over a T-shirt. The mask sits on a padded elastic strap and can be worn on the side of the head.', 'Kitsune are fox spirits from Japanese folklore, said to grow a new tail every hundred years. The haori print hides nine tails in the pattern.', 'Count the tails on the back.', 'Mask, haori and obi', 4200, NULL, 9, FALSE, NULL, NULL, TRUE, 'Size', 'ACTIVE', 'SEED', now() - interval '9 hours'
FROM categories c WHERE c.slug = 'costumes'
ON CONFLICT (slug) DO NOTHING;
INSERT INTO product_facts (product_id, label, fact_value, sort_order) SELECT p.id, v.l, v.v, v.o FROM products p, (VALUES ('Pieces', '3', 0), ('Fabric', 'Polyester crepe', 1), ('Mask', 'Resin, elastic strap', 2), ('Care', 'Hand wash cold', 3)) AS v(l, v, o) WHERE p.slug = 'kitsune-set' AND NOT EXISTS (SELECT 1 FROM product_facts f WHERE f.product_id = p.id);
INSERT INTO product_box_items (product_id, item, sort_order) SELECT p.id, v.i, v.o FROM products p, (VALUES ('Fox mask', 0), ('Haori jacket', 1), ('Obi belt', 2), ('Storage bag', 3)) AS v(i, o) WHERE p.slug = 'kitsune-set' AND NOT EXISTS (SELECT 1 FROM product_box_items b WHERE b.product_id = p.id);
INSERT INTO product_options (product_id, option_value, price_delta, stock, swatch, sort_order, created_by) SELECT p.id, v.val, v.d, v.s, v.sw, v.o, 'SEED' FROM products p, (VALUES ('S', 0, 3, NULL, 0), ('M', 0, 4, NULL, 1), ('L', 0, 2, NULL, 2), ('XL', 200, 0, NULL, 3)) AS v(val, d, s, sw, o) WHERE p.slug = 'kitsune-set' AND NOT EXISTS (SELECT 1 FROM product_options x WHERE x.product_id = p.id);

INSERT INTO products (slug, category_id, name, maker, summary, description, story, story_quote, spec_line, price, compare_at_price, stock, preorder, preorder_release, deposit, sized, option_name, status, created_by, creation_date)
SELECT 'sailor-uniform', c.id, 'Sailor school uniform set', 'Senpai Supply atelier', 'The classic sailor-collar uniform, in breathable cotton blend for Dhaka weather.', 'Who it is for: first cosplays and photo shoots.

The pleats are heat-set, so they survive washing.', 'The sailor collar came to school uniforms in the 1920s and became the shorthand for a school-story heroine.', 'The pleats stay.', 'Top, skirt, scarf', 2800, NULL, 14, FALSE, NULL, NULL, TRUE, 'Size', 'ACTIVE', 'SEED', now() - interval '8 hours'
FROM categories c WHERE c.slug = 'costumes'
ON CONFLICT (slug) DO NOTHING;
INSERT INTO product_facts (product_id, label, fact_value, sort_order) SELECT p.id, v.l, v.v, v.o FROM products p, (VALUES ('Pieces', '3', 0), ('Fabric', 'Cotton blend', 1), ('Lining', 'None, stays cool', 2), ('Care', 'Machine wash 30°C', 3)) AS v(l, v, o) WHERE p.slug = 'sailor-uniform' AND NOT EXISTS (SELECT 1 FROM product_facts f WHERE f.product_id = p.id);
INSERT INTO product_box_items (product_id, item, sort_order) SELECT p.id, v.i, v.o FROM products p, (VALUES ('Sailor top', 0), ('Pleated skirt', 1), ('Scarf', 2)) AS v(i, o) WHERE p.slug = 'sailor-uniform' AND NOT EXISTS (SELECT 1 FROM product_box_items b WHERE b.product_id = p.id);
INSERT INTO product_options (product_id, option_value, price_delta, stock, swatch, sort_order, created_by) SELECT p.id, v.val, v.d, v.s, v.sw, v.o, 'SEED' FROM products p, (VALUES ('S', 0, 4, NULL, 0), ('M', 0, 4, NULL, 1), ('L', 0, 3, NULL, 2), ('XL', 0, 3, NULL, 3)) AS v(val, d, s, sw, o) WHERE p.slug = 'sailor-uniform' AND NOT EXISTS (SELECT 1 FROM product_options x WHERE x.product_id = p.id);

INSERT INTO products (slug, category_id, name, maker, summary, description, story, story_quote, spec_line, price, compare_at_price, stock, preorder, preorder_release, deposit, sized, option_name, status, created_by, creation_date)
SELECT 'silver-wig', c.id, 'Silver long wig, heat resistant', 'Senpai Supply atelier', 'Waist-length silver wig that takes heat styling, with an adjustable cap.', 'Who it is for: any character with long silver hair.

Style with a straightener on low heat. Comb from the ends up.', 'Silver hair is the anime shorthand for someone older than they look.', 'Brush from the ends.', '80 cm, styles up to 180°C', 1350, NULL, 3, FALSE, NULL, NULL, FALSE, NULL, 'ACTIVE', 'SEED', now() - interval '7 hours'
FROM categories c WHERE c.slug = 'costumes'
ON CONFLICT (slug) DO NOTHING;
INSERT INTO product_facts (product_id, label, fact_value, sort_order) SELECT p.id, v.l, v.v, v.o FROM products p, (VALUES ('Length', '80 cm', 0), ('Fibre', 'Heat resistant to 180°C', 1), ('Cap', 'Adjustable, 54 to 58 cm', 2), ('Weight', '260 g', 3)) AS v(l, v, o) WHERE p.slug = 'silver-wig' AND NOT EXISTS (SELECT 1 FROM product_facts f WHERE f.product_id = p.id);
INSERT INTO product_box_items (product_id, item, sort_order) SELECT p.id, v.i, v.o FROM products p, (VALUES ('Wig', 0), ('Wig cap', 1), ('Hair net', 2)) AS v(i, o) WHERE p.slug = 'silver-wig' AND NOT EXISTS (SELECT 1 FROM product_box_items b WHERE b.product_id = p.id);

INSERT INTO products (slug, category_id, name, maker, summary, description, story, story_quote, spec_line, price, compare_at_price, stock, preorder, preorder_release, deposit, sized, option_name, status, created_by, creation_date)
SELECT 'sakura-bottle', c.id, 'Sakura steel bottle, 750 ml', 'Senpai Supply', 'Double-wall steel bottle printed with a falling-sakura band. Keeps water cold through a Dhaka afternoon.', 'Who it is for: students and commuters who carry water all day.

The lid seals with a quarter turn and does not leak in a bag. The print is fired on, so it does not peel in the dishwasher''s top rack.', 'The sakura band runs all the way round, so the bottle looks right from any side. Spin it to see.', 'Petals all the way round.', '750 ml, cold for 24 h', 1450, NULL, 32, FALSE, NULL, NULL, FALSE, 'Colour', 'ACTIVE', 'SEED', now() - interval '6 hours'
FROM categories c WHERE c.slug = 'bottles'
ON CONFLICT (slug) DO NOTHING;
INSERT INTO product_facts (product_id, label, fact_value, sort_order) SELECT p.id, v.l, v.v, v.o FROM products p, (VALUES ('Capacity', '750 ml', 0), ('Keeps cold', '24 hours', 1), ('Keeps hot', '12 hours', 2), ('Material', '304 stainless steel', 3)) AS v(l, v, o) WHERE p.slug = 'sakura-bottle' AND NOT EXISTS (SELECT 1 FROM product_facts f WHERE f.product_id = p.id);
INSERT INTO product_box_items (product_id, item, sort_order) SELECT p.id, v.i, v.o FROM products p, (VALUES ('Bottle', 0), ('Leak-proof lid', 1), ('Spare seal', 2)) AS v(i, o) WHERE p.slug = 'sakura-bottle' AND NOT EXISTS (SELECT 1 FROM product_box_items b WHERE b.product_id = p.id);
INSERT INTO product_options (product_id, option_value, price_delta, stock, swatch, sort_order, created_by) SELECT p.id, v.val, v.d, v.s, v.sw, v.o, 'SEED' FROM products p, (VALUES ('Petal pink', 0, 20, '#f4a6c0', 0), ('Night navy', 0, 12, '#1f2a4d', 1)) AS v(val, d, s, sw, o) WHERE p.slug = 'sakura-bottle' AND NOT EXISTS (SELECT 1 FROM product_options x WHERE x.product_id = p.id);

INSERT INTO products (slug, category_id, name, maker, summary, description, story, story_quote, spec_line, price, compare_at_price, stock, preorder, preorder_release, deposit, sized, option_name, status, created_by, creation_date)
SELECT 'mecha-shaker', c.id, 'Mecha tritan shaker, 600 ml', 'Gearframe', 'BPA-free shaker with a mixing coil, printed with Unit-07 hazard stripes.', 'Who it is for: gym days.

Scale marks every 100 ml are moulded in, so they never wear off.', 'The stripe pattern comes from the hangar floor where Unit-07 is stored.', 'Hangar stripes, gym use.', '600 ml, leak-proof lid', 950, NULL, 21, FALSE, NULL, NULL, FALSE, NULL, 'ACTIVE', 'SEED', now() - interval '5 hours'
FROM categories c WHERE c.slug = 'bottles'
ON CONFLICT (slug) DO NOTHING;
INSERT INTO product_facts (product_id, label, fact_value, sort_order) SELECT p.id, v.l, v.v, v.o FROM products p, (VALUES ('Capacity', '600 ml', 0), ('Material', 'Tritan, BPA free', 1), ('Mixer', 'Steel coil', 2), ('Dishwasher', 'Top rack', 3)) AS v(l, v, o) WHERE p.slug = 'mecha-shaker' AND NOT EXISTS (SELECT 1 FROM product_facts f WHERE f.product_id = p.id);
INSERT INTO product_box_items (product_id, item, sort_order) SELECT p.id, v.i, v.o FROM products p, (VALUES ('Shaker', 0), ('Mixing coil', 1)) AS v(i, o) WHERE p.slug = 'mecha-shaker' AND NOT EXISTS (SELECT 1 FROM product_box_items b WHERE b.product_id = p.id);

INSERT INTO products (slug, category_id, name, maker, summary, description, story, story_quote, spec_line, price, compare_at_price, stock, preorder, preorder_release, deposit, sized, option_name, status, created_by, creation_date)
SELECT 'ramen-cat-bottle', c.id, 'Ramen cat kids bottle, 500 ml', 'Pocket Parade', 'A light bottle with a flip-up straw and a cat slurping ramen round the side.', 'Who it is for: school bags.

The straw locks down so it does not leak on the way to class.', 'The cat appears in three poses as you turn the bottle: blowing, slurping, full.', 'Blow, slurp, full.', '500 ml, flip straw', 890, NULL, 2, FALSE, NULL, NULL, FALSE, NULL, 'ACTIVE', 'SEED', now() - interval '4 hours'
FROM categories c WHERE c.slug = 'bottles'
ON CONFLICT (slug) DO NOTHING;
INSERT INTO product_facts (product_id, label, fact_value, sort_order) SELECT p.id, v.l, v.v, v.o FROM products p, (VALUES ('Capacity', '500 ml', 0), ('Material', 'Tritan, BPA free', 1), ('Straw', 'Silicone, flip-up', 2), ('Age', '3+', 3)) AS v(l, v, o) WHERE p.slug = 'ramen-cat-bottle' AND NOT EXISTS (SELECT 1 FROM product_facts f WHERE f.product_id = p.id);
INSERT INTO product_box_items (product_id, item, sort_order) SELECT p.id, v.i, v.o FROM products p, (VALUES ('Bottle', 0), ('Spare straw', 1)) AS v(i, o) WHERE p.slug = 'ramen-cat-bottle' AND NOT EXISTS (SELECT 1 FROM product_box_items b WHERE b.product_id = p.id);

INSERT INTO products (slug, category_id, name, maker, summary, description, story, story_quote, spec_line, price, compare_at_price, stock, preorder, preorder_release, deposit, sized, option_name, status, created_by, creation_date)
SELECT 'ita-backpack', c.id, 'Ita backpack with clear window, 22 L', 'Senpai Supply', 'A backpack with a clear front window to show off your pins and badges.', 'Who it is for: fans who collect pins and badges.

The window panel unzips so you can change the display. Behind it, a padded laptop sleeve sits against your back.', 'Ita bags started at Japanese conventions, where fans covered bags with badges of one character. The word comes from itai, meaning painful, as in painfully devoted.', 'Painfully devoted, in a good way.', '22 L, fits a 15 in laptop', 3600, NULL, 11, FALSE, NULL, NULL, FALSE, 'Colour', 'ACTIVE', 'SEED', now() - interval '3 hours'
FROM categories c WHERE c.slug = 'bags'
ON CONFLICT (slug) DO NOTHING;
INSERT INTO product_facts (product_id, label, fact_value, sort_order) SELECT p.id, v.l, v.v, v.o FROM products p, (VALUES ('Capacity', '22 litres', 0), ('Laptop', 'Up to 15 inch', 1), ('Window', 'Removable clear panel', 2), ('Fabric', 'Water-resistant nylon', 3)) AS v(l, v, o) WHERE p.slug = 'ita-backpack' AND NOT EXISTS (SELECT 1 FROM product_facts f WHERE f.product_id = p.id);
INSERT INTO product_box_items (product_id, item, sort_order) SELECT p.id, v.i, v.o FROM products p, (VALUES ('Backpack', 0), ('Pin board insert', 1)) AS v(i, o) WHERE p.slug = 'ita-backpack' AND NOT EXISTS (SELECT 1 FROM product_box_items b WHERE b.product_id = p.id);
INSERT INTO product_options (product_id, option_value, price_delta, stock, swatch, sort_order, created_by) SELECT p.id, v.val, v.d, v.s, v.sw, v.o, 'SEED' FROM products p, (VALUES ('Black', 0, 6, '#1b1b22', 0), ('Sakura pink', 0, 5, '#f4a6c0', 1)) AS v(val, d, s, sw, o) WHERE p.slug = 'ita-backpack' AND NOT EXISTS (SELECT 1 FROM product_options x WHERE x.product_id = p.id);

INSERT INTO products (slug, category_id, name, maker, summary, description, story, story_quote, spec_line, price, compare_at_price, stock, preorder, preorder_release, deposit, sized, option_name, status, created_by, creation_date)
SELECT 'canvas-tote', c.id, 'Shonen panel canvas tote', 'Senpai Supply', 'A heavy canvas tote printed with a full manga page.', 'Who it is for: everyday errands.

The handles are long enough to wear on the shoulder.', 'The printed page reads right to left, the way manga does.', 'Read it right to left.', '12 oz canvas, inner pocket', 990, NULL, 40, FALSE, NULL, NULL, FALSE, NULL, 'ACTIVE', 'SEED', now() - interval '2 hours'
FROM categories c WHERE c.slug = 'bags'
ON CONFLICT (slug) DO NOTHING;
INSERT INTO product_facts (product_id, label, fact_value, sort_order) SELECT p.id, v.l, v.v, v.o FROM products p, (VALUES ('Size', '38 by 42 cm', 0), ('Fabric', '12 oz cotton canvas', 1), ('Handles', '65 cm', 2), ('Pocket', 'Inner zip pocket', 3)) AS v(l, v, o) WHERE p.slug = 'canvas-tote' AND NOT EXISTS (SELECT 1 FROM product_facts f WHERE f.product_id = p.id);
INSERT INTO product_box_items (product_id, item, sort_order) SELECT p.id, v.i, v.o FROM products p, (VALUES ('Tote', 0)) AS v(i, o) WHERE p.slug = 'canvas-tote' AND NOT EXISTS (SELECT 1 FROM product_box_items b WHERE b.product_id = p.id);

INSERT INTO products (slug, category_id, name, maker, summary, description, story, story_quote, spec_line, price, compare_at_price, stock, preorder, preorder_release, deposit, sized, option_name, status, created_by, creation_date)
SELECT 'kunai-sling', c.id, 'Kunai sling bag', 'Senpai Supply', 'A slim cross-body bag with a hidden phone pocket against your back.', 'Who it is for: crowded markets and conventions.

The phone pocket opens only from the side that faces you.', 'Shaped after a kunai''s handle wrap, with the same diagonal stitching.', 'Wrapped like a kunai handle.', '5 L, cross-body', 1650, NULL, 25, TRUE, '2026-12-01', 500, FALSE, NULL, 'ACTIVE', 'SEED', now() - interval '1 hours'
FROM categories c WHERE c.slug = 'bags'
ON CONFLICT (slug) DO NOTHING;
INSERT INTO product_facts (product_id, label, fact_value, sort_order) SELECT p.id, v.l, v.v, v.o FROM products p, (VALUES ('Capacity', '5 litres', 0), ('Strap', 'Adjustable, 70 to 130 cm', 1), ('Pockets', '3, one hidden', 2), ('Fabric', 'Ripstop nylon', 3)) AS v(l, v, o) WHERE p.slug = 'kunai-sling' AND NOT EXISTS (SELECT 1 FROM product_facts f WHERE f.product_id = p.id);
INSERT INTO product_box_items (product_id, item, sort_order) SELECT p.id, v.i, v.o FROM products p, (VALUES ('Sling bag', 0)) AS v(i, o) WHERE p.slug = 'kunai-sling' AND NOT EXISTS (SELECT 1 FROM product_box_items b WHERE b.product_id = p.id);

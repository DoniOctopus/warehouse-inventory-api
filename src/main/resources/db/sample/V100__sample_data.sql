-- Demo data, loaded only when classpath:db/sample is in spring.flyway.locations (the default).
-- Ids are not hard-coded: rows are linked through their natural keys (item name / SKU).

INSERT INTO items (name, description, base_price, created_at, updated_at)
VALUES ('Basic T-Shirt', 'Unisex 100% cotton crew-neck T-shirt', 99000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('Denim Jeans', 'Slim-fit stretch denim', 349000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('Canvas Tote Bag', 'Heavy canvas tote with inner pocket', 75000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('Ceramic Coffee Mug', '350 ml glazed ceramic mug', 55000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- price NULL = inherits the item's base price
INSERT INTO variants (item_id, sku, name, price, stock_quantity, created_at, updated_at)
VALUES ((SELECT id FROM items WHERE name = 'Basic T-Shirt'), 'TS-BLK-S', 'Black / S', NULL, 15, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ((SELECT id FROM items WHERE name = 'Basic T-Shirt'), 'TS-BLK-M', 'Black / M', NULL, 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ((SELECT id FROM items WHERE name = 'Basic T-Shirt'), 'TS-BLK-XL', 'Black / XL', 109000.00, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ((SELECT id FROM items WHERE name = 'Basic T-Shirt'), 'TS-WHT-M', 'White / M', NULL, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ((SELECT id FROM items WHERE name = 'Denim Jeans'), 'JN-IND-30', 'Indigo / W30', NULL, 8, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ((SELECT id FROM items WHERE name = 'Denim Jeans'), 'JN-IND-32', 'Indigo / W32', NULL, 12, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ((SELECT id FROM items WHERE name = 'Denim Jeans'), 'JN-BLK-32', 'Black / W32', 369000.00, 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ((SELECT id FROM items WHERE name = 'Canvas Tote Bag'), 'TOTE-NAT', 'Natural', NULL, 25, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ((SELECT id FROM items WHERE name = 'Ceramic Coffee Mug'), 'MUG-WHT', 'White', NULL, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ((SELECT id FROM items WHERE name = 'Ceramic Coffee Mug'), 'MUG-BLK', 'Matte Black', 60000.00, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO variant_attributes (variant_id, attr_name, attr_value)
SELECT v.id, a.attr_name, a.attr_value
FROM variants v
         JOIN (VALUES ('TS-BLK-S', 'color', 'Black'), ('TS-BLK-S', 'size', 'S'),
                      ('TS-BLK-M', 'color', 'Black'), ('TS-BLK-M', 'size', 'M'),
                      ('TS-BLK-XL', 'color', 'Black'), ('TS-BLK-XL', 'size', 'XL'),
                      ('TS-WHT-M', 'color', 'White'), ('TS-WHT-M', 'size', 'M'),
                      ('JN-IND-30', 'color', 'Indigo'), ('JN-IND-30', 'waist', '30'),
                      ('JN-IND-32', 'color', 'Indigo'), ('JN-IND-32', 'waist', '32'),
                      ('JN-BLK-32', 'color', 'Black'), ('JN-BLK-32', 'waist', '32'),
                      ('TOTE-NAT', 'color', 'Natural'),
                      ('MUG-WHT', 'color', 'White'),
                      ('MUG-BLK', 'color', 'Black'), ('MUG-BLK', 'finish', 'Matte'))
    AS a(sku, attr_name, attr_value) ON a.sku = v.sku;

-- Keep the ledger consistent with the seeded stock levels.
INSERT INTO stock_movements (variant_id, type, quantity_change, quantity_after, reason, created_at)
SELECT id, 'INITIAL', stock_quantity, stock_quantity, 'Sample data', CURRENT_TIMESTAMP
FROM variants
WHERE stock_quantity > 0;

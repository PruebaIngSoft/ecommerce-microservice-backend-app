-- Insertar categorías de prueba
INSERT INTO categories (category_title, image_url, created_at) VALUES
('Computer', 'https://example.com/computer.jpg', CURRENT_TIMESTAMP),
('Mode', 'https://example.com/mode.jpg', CURRENT_TIMESTAMP),
('Game', 'https://example.com/game.jpg', CURRENT_TIMESTAMP);

-- Insertar productos de prueba
INSERT INTO products (category_id, product_title, image_url, sku, price_unit, quantity, created_at) VALUES
(1, 'ASUS Laptop', 'https://example.com/asus.jpg', 'ASUS-001', 999.99, 50, CURRENT_TIMESTAMP),
(1, 'HP Laptop', 'https://example.com/hp.jpg', 'HP-001', 899.99, 50, CURRENT_TIMESTAMP),
(2, 'Armani Shirt', 'https://example.com/armani.jpg', 'ARM-001', 149.99, 50, CURRENT_TIMESTAMP),
(3, 'GTA V', 'https://example.com/gta.jpg', 'GTA-001', 59.99, 50, CURRENT_TIMESTAMP);

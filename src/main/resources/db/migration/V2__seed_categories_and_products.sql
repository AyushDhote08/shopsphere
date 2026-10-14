-- ShopSphere Flyway Migration V2: Initial Sample Categories and Products

INSERT INTO categories (id, name, slug, description, parent_id, is_active) VALUES
(1, 'Electronics', 'electronics', 'Consumer electronics, gadgets, and tech devices', NULL, TRUE),
(2, 'Audio & Sound', 'audio-sound', 'Headphones, earphones, soundbars, and studio monitors', 1, TRUE),
(3, 'Computing & Laptops', 'computing-laptops', 'High-performance laptops, desktops, and computer peripherals', 1, TRUE),
(4, 'Wearable Tech', 'wearable-tech', 'Smartwatches, fitness bands, and wearable sensors', 1, TRUE),
(5, 'Accessories', 'accessories', 'Cables, chargers, mounts, and protective gear', 1, TRUE);

INSERT INTO products (id, sku, name, slug, description, cost_price, selling_price, stock_quantity, low_stock_threshold, image_url, category_id, is_active) VALUES
(1, 'ELEC-AUD-001', 'AcousticPro ANC Wireless Headphones', 'acousticpro-anc-wireless-headphones', 'Over-ear active noise-cancelling headphones with 45-hour battery life and 40mm beryllium drivers.', 85.00, 199.99, 45, 5, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600', 2, TRUE),
(2, 'ELEC-AUD-002', 'SoundPulse Waterproof Bluetooth Speaker', 'soundpulse-waterproof-bluetooth-speaker', 'IPX7 portable waterproof speaker with deep bass and 360-degree sound projection.', 28.50, 69.99, 80, 10, 'https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=600', 2, TRUE),
(3, 'ELEC-AUD-003', 'StudioMaster Reference In-Ear Monitors', 'studiomaster-reference-in-ear-monitors', 'Dual-driver balanced armature in-ear monitors tailored for musicians and audiophiles.', 55.00, 129.50, 30, 5, 'https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=600', 2, TRUE),
(4, 'ELEC-CMP-001', 'ProBook Aero 14-inch Ultrabook', 'probook-aero-14-ultrabook', 'Lightweight Intel Core Ultra laptop with 32GB LPDDR5X RAM, 1TB NVMe SSD, and 120Hz OLED display.', 750.00, 1199.00, 18, 4, 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=600', 3, TRUE),
(5, 'ELEC-CMP-002', 'Mechanical RGB Gaming Keyboard (Tactile)', 'mechanical-rgb-gaming-keyboard', 'Custom hot-swappable mechanical keyboard featuring lubricated tactile switches and PBT keycaps.', 42.00, 99.99, 50, 8, 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=600', 3, TRUE),
(6, 'ELEC-CMP-002-M', 'ErgoPrecision Wireless Vertical Mouse', 'ergoprecision-wireless-vertical-mouse', 'Scientifically sculpted ergonomic mouse reducing wrist strain with adjustable 4000 DPI sensor.', 22.00, 49.99, 65, 10, 'https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=600', 3, TRUE),
(7, 'ELEC-CMP-004', '4K UHD UltraWide Curved Monitor 34-inch', '4k-uhd-ultrawide-curved-monitor-34', 'Immersive 34-inch 144Hz curved HDR display with USB-C 90W power delivery.', 320.00, 549.99, 12, 3, 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=600', 3, TRUE),
(8, 'ELEC-WEA-001', 'PulseFit Elite Smartwatch', 'pulsefit-elite-smartwatch', 'Titanium GPS fitness smartwatch with continuous SpO2, heart rate, and 14-day battery life.', 95.00, 219.00, 35, 5, 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600', 4, TRUE),
(9, 'ELEC-WEA-002', 'ActiveBand Lightweight Fitness Tracker', 'activeband-lightweight-fitness-tracker', 'Subtle, water-resistant fitness tracker with sleep staging and auto-workout detection.', 18.00, 39.99, 110, 15, 'https://images.unsplash.com/photo-1575311373937-040b8e1fd5b6?w=600', 4, TRUE),
(10, 'ELEC-ACC-001', 'HyperCharge 100W GaN Fast Charger', 'hypercharge-100w-gan-fast-charger', 'Multi-port Gallium Nitride charger capable of simultaneously powering laptops and phones.', 19.50, 45.00, 90, 10, 'https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=600', 5, TRUE),
(11, 'ELEC-ACC-002', 'ThunderBolt 4 Braided Cable (2m)', 'thunderbolt-4-braided-cable-2m', 'Certified 40Gbps data transfer, 100W PD charging, and 8K video output supported braided cable.', 9.00, 24.99, 150, 20, 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600', 5, TRUE),
(12, 'ELEC-ACC-003', 'Magnetic Aluminum Laptop Stand', 'magnetic-aluminum-laptop-stand', 'Foldable CNC-machined aluminum stand providing optimal ergonomics and passive heat dissipation.', 14.00, 34.99, 75, 10, 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=600', 5, TRUE);

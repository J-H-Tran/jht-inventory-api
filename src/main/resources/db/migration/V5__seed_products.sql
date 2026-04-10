-- Seed products for testing ledger patterns
-- Minimal catalog: diverse units of measure, mix of active/inactive

INSERT INTO products (id, sku, name, unit_of_measure, active, created_at, updated_at)
VALUES
-- Electronics - EACH
('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'LAPTOP-001', 'Business Laptop Pro 15"', 'EACH', true, now(), now()),
('b2c3d4e5-f6a7-8901-bcde-f23456789012', 'PHONE-001', 'Smartphone 128GB Black', 'EACH', true, now(), now()),
('c3d4e5f6-a7b8-9012-cdef-345678901234', 'TABLET-001', 'Tablet 10" WiFi', 'EACH', false, now(), now()), -- inactive

-- Raw materials - KG
('d4e5f6a7-b8c9-0123-defa-456789012345', 'STEEL-001', 'Carbon Steel Sheet Grade A', 'KG', true, now(), now()),
('e5f6a7b8-c9d0-1234-efab-567890123456', 'PLASTIC-001', 'ABS Plastic Pellets', 'KG', true, now(), now()),

-- Liquids - LITRE
('f6a7b8c9-d0e1-2345-fabc-678901234567', 'OIL-001', 'Machine Oil Industrial Grade', 'LITRE', true, now(), now()),
('a7b8c9d0-e1f2-3456-abcd-789012345678', 'ADHESIVE-001', 'Epoxy Resin 2-Part', 'LITRE', true, now(), now()),

-- Length goods - METRE
('b8c9d0e1-f2a3-4567-bcde-890123456789', 'CABLE-001', 'Ethernet Cable Cat6', 'METRE', true, now(), now()),
('c9d0e1f2-a3b4-5678-cdef-901234567890', 'PIPE-001', 'PVC Pipe 50mm Diameter', 'METRE', true, now(), now()),

-- Packaging/containers - BOX, PALLET
('d0e1f2a3-b4c5-6789-defa-012345678901', 'BOX-001', 'Corrugated Box 300x200x150mm', 'BOX', true, now(), now()),
('e1f2a3b4-c5d6-7890-efab-123456789012', 'PALLET-001', 'Wooden Pallet Standard', 'PALLET', true, now(), now());

COMMENT ON TABLE products IS 'Product catalog seeded with diverse UOMs for ledger testing';
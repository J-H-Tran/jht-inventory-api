-- ============================================
-- LAPTOP-001: Clean receive and ship cycle
-- ============================================
INSERT INTO inventory_ledger (id, product_id, movement_type, quantity_delta, idempotency_key, reference_id, reference_type, occurred_at, created_by, note)
VALUES
    -- Initial stock receipt (100 units)
    ('11111111-1111-1111-1111-111111111111', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'RECEIVE', 100, 'rcpt-2024-001-laptop', '11111111-1111-1111-1111-111111111112', 'PURCHASE_ORDER', now() - interval '30 days', 'system', 'Initial stock receipt from supplier'),

    -- Customer order shipments
    ('22222222-2222-2222-2222-222222222222', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'SHIP', -5, 'ship-2024-001-laptop', '22222222-2222-2222-2222-222222222223', 'SALES_ORDER', now() - interval '25 days', 'warehouse_a', 'Shipped to Customer A'),
    ('33333333-3333-3333-3333-333333333333', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'SHIP', -3, 'ship-2024-002-laptop', '33333333-3333-3333-3333-333333333334', 'SALES_ORDER', now() - interval '20 days', 'warehouse_a', 'Shipped to Customer B'),
    ('44444444-4444-4444-4444-444444444444', 'a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'SHIP', -10, 'ship-2024-003-laptop', '44444444-4444-4444-4444-444444444445', 'SALES_ORDER', now() - interval '15 days', 'warehouse_a', 'Bulk order Customer C');

-- ============================================
-- STEEL-001: Receipts with damage adjustment
-- ============================================
INSERT INTO inventory_ledger (id, product_id, movement_type, quantity_delta, reason_code, idempotency_key, reference_id, reference_type, occurred_at, created_by, note)
VALUES
    -- Large receipt (5000 kg)
    ('55555555-5555-5555-5555-555555555555', 'd4e5f6a7-b8c9-0123-defa-456789012345', 'RECEIVE', 5000, null, 'rcpt-2024-002-steel', '55555555-5555-5555-5555-555555555556', 'PURCHASE_ORDER', now() - interval '28 days', 'system', 'Mill delivery batch A'),

    -- Damage discovered during inspection (negative adjustment)
    ('66666666-6666-6666-6666-666666666666', 'd4e5f6a7-b8c9-0123-defa-456789012345', 'ADJUST', -150, 'DAMAGE', 'adj-2024-001-steel', null, null, now() - interval '27 days', 'inspector_jones', 'Rust damage on 3 pallets - scrapped'),

    -- Partial shipment to production
    ('77777777-7777-7777-7777-777777777777', 'd4e5f6a7-b8c9-0123-defa-456789012345', 'SHIP', -2000, null, 'ship-2024-004-steel', '77777777-7777-7777-7777-777777777778', 'WORK_ORDER', now() - interval '20 days', 'warehouse_b', 'Transfer to production line 1');

-- ============================================
-- OIL-001: Multiple receipts, consumption, cycle count adjustment
-- ============================================
INSERT INTO inventory_ledger (id, product_id, movement_type, quantity_delta, reason_code, idempotency_key, reference_id, reference_type, occurred_at, created_by, note)
VALUES
    -- First receipt
    ('88888888-8888-8888-8888-888888888888', 'f6a7b8c9-d0e1-2345-fabc-678901234567', 'RECEIVE', 200, null, 'rcpt-2024-003-oil', '88888888-8888-8888-8888-888888888889', 'PURCHASE_ORDER', now() - interval '45 days', 'system', 'Drum delivery - 200L'),

    -- Second receipt (partial)
    ('99999999-9999-9999-9999-999999999999', 'f6a7b8c9-d0e1-2345-fabc-678901234567', 'RECEIVE', 150, null, 'rcpt-2024-004-oil', '99999999-9999-9999-9999-99999999999a', 'PURCHASE_ORDER', now() - interval '30 days', 'system', 'Top-up delivery'),

    -- Production consumption
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'f6a7b8c9-d0e1-2345-fabc-678901234567', 'SHIP', -80, null, 'ship-2024-005-oil', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab', 'WORK_ORDER', now() - interval '25 days', 'warehouse_c', 'Weekly production batch'),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'f6a7b8c9-d0e1-2345-fabc-678901234567', 'SHIP', -75, null, 'ship-2024-006-oil', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbc', 'WORK_ORDER', now() - interval '18 days', 'warehouse_c', 'Weekly production batch'),
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'f6a7b8c9-d0e1-2345-fabc-678901234567', 'SHIP', -90, null, 'ship-2024-007-oil', 'cccccccc-cccc-cccc-cccc-cccccccccccd', 'WORK_ORDER', now() - interval '11 days', 'warehouse_c', 'Weekly production batch'),

    -- Cycle count found discrepancy (using AUDIT_CORRECTION instead of EVAPORATION)
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'f6a7b8c9-d0e1-2345-fabc-678901234567', 'ADJUST', -12, 'AUDIT_CORRECTION', 'adj-2024-002-oil', null, null, now() - interval '7 days', 'warehouse_c', 'Monthly cycle count - evaporation loss');

-- ============================================
-- CABLE-001: Partial shipments, no adjustments (clean history)
-- ============================================
INSERT INTO inventory_ledger (id, product_id, movement_type, quantity_delta, reason_code, idempotency_key, reference_id, reference_type, occurred_at, created_by, note)
VALUES
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'b8c9d0e1-f2a3-4567-bcde-890123456789', 'RECEIVE', 1000, null, 'rcpt-2024-005-cable', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeef', 'PURCHASE_ORDER', now() - interval '35 days', 'system', 'Spool delivery 1000m'),
    ('ffffffff-ffff-ffff-ffff-ffffffffffff', 'b8c9d0e1-f2a3-4567-bcde-890123456789', 'SHIP', -50, null, 'ship-2024-008-cable', 'ffffffff-ffff-ffff-ffff-fffffffffff0', 'SALES_ORDER', now() - interval '28 days', 'warehouse_a', 'Custom cut order'),
    ('11111111-2222-3333-4444-555555555555', 'b8c9d0e1-f2a3-4567-bcde-890123456789', 'SHIP', -200, null, 'ship-2024-009-cable', '11111111-2222-3333-4444-555555555556', 'SALES_ORDER', now() - interval '21 days', 'warehouse_a', 'Contractor bulk order'),
    ('66666666-7777-8888-9999-000000000000', 'b8c9d0e1-f2a3-4567-bcde-890123456789', 'SHIP', -25, null, 'ship-2024-010-cable', '66666666-7777-8888-9999-000000000001', 'SALES_ORDER', now() - interval '14 days', 'warehouse_a', 'Retail walk-in');

-- ============================================
-- BOX-001: High velocity, many small shipments
-- ============================================
INSERT INTO inventory_ledger (id, product_id, movement_type, quantity_delta, reason_code, idempotency_key, reference_id, reference_type, occurred_at, created_by, note)
VALUES
    ('22222222-3333-4444-5555-666666666666', 'd0e1f2a3-b4c5-6789-defa-012345678901', 'RECEIVE', 500, null, 'rcpt-2024-006-box', '22222222-3333-4444-5555-666666666667', 'PURCHASE_ORDER', now() - interval '40 days', 'system', 'Packaging supplier delivery'),
    ('33333333-4444-5555-6666-777777777777', 'd0e1f2a3-b4c5-6789-defa-012345678901', 'SHIP', -50, null, 'ship-2024-011-box', '33333333-4444-5555-6666-777777777778', 'SALES_ORDER', now() - interval '35 days', 'warehouse_a', 'Packaging for laptop orders'),
    ('44444444-5555-6666-7777-888888888888', 'd0e1f2a3-b4c5-6789-defa-012345678901', 'SHIP', -50, null, 'ship-2024-012-box', '44444444-5555-6666-7777-888888888889', 'SALES_ORDER', now() - interval '30 days', 'warehouse_a', 'Packaging for phone orders'),
    ('55555555-6666-7777-8888-999999999999', 'd0e1f2a3-b4c5-6789-defa-012345678901', 'SHIP', -100, null, 'ship-2024-013-box', '55555555-6666-7777-8888-99999999999a', 'SALES_ORDER', now() - interval '25 days', 'warehouse_a', 'Bulk packaging order'),
    ('66666666-7777-8888-9999-000000000001', 'd0e1f2a3-b4c5-6789-defa-012345678901', 'SHIP', -75, null, 'ship-2024-014-box', '66666666-7777-8888-9999-000000000002', 'SALES_ORDER', now() - interval '20 days', 'warehouse_a', 'Mixed product shipment'),
    ('77777777-8888-9999-0000-111111111112', 'd0e1f2a3-b4c5-6789-defa-012345678901', 'SHIP', -60, null, 'ship-2024-015-box', '77777777-8888-9999-0000-111111111113', 'SALES_ORDER', now() - interval '15 days', 'warehouse_a', 'Express order fulfillment');
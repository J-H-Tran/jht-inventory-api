-- Step 1: Drop the old incorrect unique constraint
ALTER TABLE inventory_items
DROP CONSTRAINT IF EXISTS inventory_items_product_id_key;

-- Step 2: Add correct composite unique constraint
ALTER TABLE inventory_items
    ADD CONSTRAINT uq_inventory_product_location
        UNIQUE (product_id, location_id);
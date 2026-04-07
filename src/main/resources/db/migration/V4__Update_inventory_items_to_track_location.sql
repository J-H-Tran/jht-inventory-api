ALTER TABLE inventory_items
ADD COLUMN location_id UUID NOT NULL REFERENCES locations(id);
ALTER TABLE inventory_movements
ADD COLUMN location_id UUID NOT NULL REFERENCES locations(id);
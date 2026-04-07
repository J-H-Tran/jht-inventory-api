ALTER TABLE reservations
ADD COLUMN location_id UUID REFERENCES locations(id);
-- Seed data for adjustment_reasons controlled vocabulary.
-- This is a versioned migration (not repeatable) because these are
-- foundational codes the application logic may reference by name.
-- New operational codes can be added in future migrations without touching this file.
insert into adjustment_reasons (code, description) values
('INITIAL_STOCK',       'First-time stock entry for a new product'),
('DAMAGE',              'Stock removed due to damage or spoilage'),
('THEFT',               'Stock removed due to confirmed or suspected theft'),
('AUDIT_CORRECTION',    'Correction following a physical stock count'),
('RETURN_TO_SUPPLIER',  'Stock returned to supplier'),
('FOUND_STOCK',         'Stock located that was previously unaccounted for'),
('SYSTEM_CORRECTION',   'Correction applied to resolve a system-detected discrepancy');
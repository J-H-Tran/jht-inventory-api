create index idx_ledger_product_occurred_id
on inventory_ledger (product_id, occurred_at desc, id desc);
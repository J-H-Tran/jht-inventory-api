-- Products are the catalog anchor for inventory.
-- Deliberately minimal: SKU, name, unit of measure.
-- No pricing, no categories, no supplier - not inventory concerns.
create table products (
    id                  uuid            primary key default gen_random_uuid(),
    sku                 varchar(100)    not null,
    name                varchar(255)    not null,
    unit_of_measure     varchar(30)     not null,
    active              boolean not     null default true,
    created_at          timestamptz     not null default now(),
    updated_at          timestamptz     not null default now(),

    constraint uq_products_sku unique (sku),
    constraint chk_products_sku_not_blank check (trim(sku) <> ''),
    constraint chk_products_name_not_blank check (trim(name) <> ''),
    constraint chk_products_uom check (unit_of_measure in ('EACH', 'KG', 'LITRE', 'METRE', 'BOX', 'PALLET'))
);

comment on table products is 'Product catalog. Inventory is tracked against these records.';
comment on column products.unit_of_measure is 'Controlled vocabulary - drives quantity semantics in the ledger.';
comment on column products.active is 'Soft delete. Inactive products retain ledger history but block new movements.';
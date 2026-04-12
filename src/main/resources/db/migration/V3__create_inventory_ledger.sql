-- The append-only ledger. Every inventory change is a row here.
-- Stock levels are derived: select sum(quantity_delta) where product_id = ?
-- No rows are ever updated or deleted.
create table inventory_ledger (
    id                  uuid            primary key default gen_random_uuid(),
    product_id          uuid            not null,
    movement_type       varchar(30)     not null,
    quantity_delta      integer         not null,
    reason_code         varchar(50),
    reference_id        uuid,
    reference_type      varchar(30),
    idempotency_key     varchar(100)    not null,
    occurred_at         timestamptz     not null default now(),
    created_by          varchar(100),
    note                text,

    -- Referential integrity
    constraint fk_ledger_product foreign key (product_id) references products(id),
    constraint fk_ledger_reason foreign key (reason_code) references adjustment_reasons(code),

    -- Idempotency guarantee: enforced at DB layer, not just application layer
    constraint uq_ledger_idempotency_key unique (idempotency_key),

    -- Movement type is a controlled set
    constraint chk_ledger_movement_type check (movement_type in ('RECEIVE', 'SHIP', 'ADJUST')),

    -- ADJUST requires a reason_code; RECEIVE and SHIP must not have one
    constraint chk_ledger_reason_code_required check (
            (movement_type = 'ADJUST' and reason_code is not null)
            or (movement_type <> 'ADJUST' and reason_code is null)
        ),

    -- quantity_delta sign must match movement intent
    -- RECEIVE must be positive, SHIP must be negative, ADJUST can be either (not zero)
    constraint chk_ledger_receive_positive check (movement_type <> 'RECEIVE' or quantity_delta > 0),
    constraint chk_ledger_ship_negative check (movement_type <> 'SHIP' or quantity_delta < 0),
    constraint chk_ledger_adjust_nonzero check (movement_type <> 'ADJUST' or quantity_delta <> 0),

    -- reference_type required if reference_id is set
    constraint chk_ledger_reference_consistency check (
            (reference_id is null and reference_type is null)
            or (reference_id is not null and reference_type is not null)
        )
);
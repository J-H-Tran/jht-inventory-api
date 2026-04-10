-- Controlled vocabulary for inventory adjustments.
-- Stored as a table rather than a Java enum for two reasons:
--   1. New reason codes can be added via V__migration without a code deployment
--   2. The ledger FK enforces valid values at the DB layer, not just the application layer
create table adjustment_reasons (
    code        varchar(50)     primary key,
    description text            not null,
    active      boolean         not null default true,

    constraint chk_adjustment_reasons_code_upper check (code = upper(code))
);

comment on table adjustment_reasons is 'Controlled vocabulary for ADJUST-type ledger entries. Required when movement_type = ADJUST.';
comment on column adjustment_reasons.code is 'Uppercase snake_case. e.g. DAMABE, AUDIT_CORRECTION. Stored in ledger as FK.';
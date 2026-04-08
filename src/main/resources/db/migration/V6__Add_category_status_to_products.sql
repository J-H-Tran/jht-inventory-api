-- Step 1: add category column
ALTER TABLE products
add column category text;

-- Step 2: Add status column
ALTER TABLE products
add column status text not null default 'ACTIVE';

-- Step 3: Add index category status
create index idx_products_category_status
on products(category, status);
-- Update existing products with purchase price as 70% of selling price
-- This sets purchase_price = price * 0.7 for all existing products

UPDATE products 
SET purchase_price = ROUND(price * 0.7, 2)
WHERE purchase_price IS NULL OR purchase_price = 0;

-- Verify the update
SELECT 
    id,
    name,
    price,
    purchase_price,
    ROUND((price - purchase_price), 2) as profit_per_unit,
    ROUND(((price - purchase_price) / price * 100), 2) as profit_margin_percentage
FROM products 
WHERE purchase_price IS NOT NULL
ORDER BY id;

-- Optional: Show summary
SELECT 
    COUNT(*) as total_products,
    SUM(price) as total_selling_value,
    SUM(purchase_price) as total_purchase_value,
    SUM(price - purchase_price) as total_profit_potential
FROM products 
WHERE purchase_price IS NOT NULL; 
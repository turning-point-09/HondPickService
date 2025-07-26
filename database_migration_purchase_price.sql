-- Database Migration Script
-- Add purchase_price column to products table for profit calculation

-- Add the purchase_price column
ALTER TABLE products 
ADD COLUMN purchase_price DECIMAL(19,2) DEFAULT NULL;

-- Optional: Add an index for better performance on profit calculations
CREATE INDEX idx_products_purchase_price ON products(purchase_price);

-- Note: You can update existing products with their purchase prices manually
-- Example: UPDATE products SET purchase_price = price * 0.7 WHERE id = 1; 
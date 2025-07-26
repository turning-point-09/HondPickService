-- Database Migration Script
-- Add created_at and updated_at columns to users table

-- Add the missing columns
ALTER TABLE users 
ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- Update existing records to have proper timestamps
-- This sets all existing users to have been created "now" 
-- You can adjust this if you want to set specific dates for existing users
UPDATE users 
SET created_at = CURRENT_TIMESTAMP, 
    updated_at = CURRENT_TIMESTAMP 
WHERE created_at IS NULL OR updated_at IS NULL;

-- Optional: Add indexes for better performance
CREATE INDEX idx_users_created_at ON users(created_at);
CREATE INDEX idx_users_updated_at ON users(updated_at); 
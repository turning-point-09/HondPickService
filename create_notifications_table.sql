-- Create admin_notifications table for storing admin notifications
CREATE TABLE admin_notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    type VARCHAR(50) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    order_id BIGINT,
    customer_name VARCHAR(100),
    customer_mobile VARCHAR(15),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at),
    INDEX idx_type (type),
    INDEX idx_order_id (order_id)
);

-- Insert a sample notification to test
INSERT INTO admin_notifications (title, message, type, order_id, customer_name, customer_mobile) 
VALUES ('System Ready', 'Admin notification system is now active!', 'SYSTEM', NULL, 'System', 'N/A'); 
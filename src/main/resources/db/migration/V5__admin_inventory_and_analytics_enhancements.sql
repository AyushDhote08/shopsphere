-- ShopSphere Flyway Migration V5: Admin Inventory & Analytics Enhancements

-- 1. Add unit_cost_snapshot to order_items to support exact historical gross profit calculations
ALTER TABLE order_items
    ADD COLUMN unit_cost_snapshot DECIMAL(12, 2) NULL;

-- Standard ANSI SQL correlated subquery for cross-database compatibility (MySQL & H2)
UPDATE order_items
SET unit_cost_snapshot = (
    SELECT p.cost_price
    FROM products p
    WHERE p.id = order_items.product_id
)
WHERE unit_cost_snapshot IS NULL;

-- 2. Add stock_before and user_id to inventory_audit_log for traceability and audit history
ALTER TABLE inventory_audit_log
    ADD COLUMN stock_before INT NULL;

ALTER TABLE inventory_audit_log
    ADD COLUMN user_id BIGINT NULL;

ALTER TABLE inventory_audit_log
    ADD CONSTRAINT fk_inventory_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

-- Backfill stock_before for existing audit log records
UPDATE inventory_audit_log
SET stock_before = stock_after - quantity_changed
WHERE stock_before IS NULL;

-- 3. Reporting and analytics optimization indexes
CREATE INDEX idx_orders_status_created ON orders(order_status, created_at);
CREATE INDEX idx_inventory_audit_created ON inventory_audit_log(created_at);
CREATE INDEX idx_inventory_audit_movement ON inventory_audit_log(movement_type);

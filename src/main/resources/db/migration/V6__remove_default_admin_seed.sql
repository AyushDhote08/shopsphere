-- ShopSphere Flyway Migration V6: Remove hardcoded default admin seed from baseline schema
-- Ensures production deployments never retain the demo admin account with documented credentials.
-- Development environments restore this account via DevAdminDataLoader if the 'dev' profile is active.

DELETE FROM addresses WHERE user_id IN (SELECT id FROM users WHERE email = 'admin@shopsphere.com');
DELETE FROM users WHERE email = 'admin@shopsphere.com' AND role = 'ROLE_ADMIN';

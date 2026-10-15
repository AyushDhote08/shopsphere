# ShopSphere — Implementation Status & Phasing Tracker

## 1. Project Phase Roadmap

| Phase | Description | Status | Completion Target |
|---|---|---|---|
| **Phase 0** | Requirements, Environment, Architecture, Git Setup, Project Baseline | **Completed** | Baseline verified & tested |
| **Phase 1** | Database Migrations & Backend Foundation (Catalog, Categories, DTOs, APIs) | **Completed** | 36/36 tests passing & MySQL verified |
| **Phase 2** | Authentication & Security (Spring Security, Sessions, BCrypt, Roles, CSRF, Auth UI) | **Completed** | 59/59 tests passing & live session verified |
| **Phase 3** | Product Catalog & Categories (Frontend UI & Storefront Integration) | **Completed** | 67/67 tests passing & live storefront verified |
| **Phase 4** | Shopping Cart, Checkout Simulation, and Order Management | **Completed** | Persistent Cart + Pessimistic Locking + 91/91 tests passing |
| **Phase 5** | Admin Dashboard, Inventory Management, and Business Analytics | **Completed** | 128/128 tests passing + Live Admin Portal & Restocking verified |
| **Phase 6** | Comprehensive Testing, Security Audit, Bug Fixes & Production Readiness | **Completed** | 144/144 tests passing + Security Hardening + Actuator + E2E Workflow verified |
| **Phase 7** | Deployment Preparation and Portfolio Launch | **Completed (Prep & Portfolio)** | JAR verified running locally, Dockerfile added, Railway/Render docs ready |
| **Phase 8** | GitHub Publication & Cloud Deployment Execution | In Progress (GitHub Prep) | Local audit clean, 144 tests passing, secrets sanitized in working tree |

---

## 2. Current Status Summary

- **Current Active Phase**: **GitHub Publication Preparation (Phase 8)**
  - Local repository audit complete: Git status checked, `.gitignore` updated with dump/backup rules and `.env` exclusions.
  - Working tree sanitized: `application-dev.yml` hardcoded fallback password replaced with empty default `${DB_PASSWORD:}`.
  - Clone-readiness confirmed: `.env.example` provides explicit template for dev and production deployments.
  - Automated tests and packaging: `mvn clean verify` passed with **144 tests run, 0 failures, 0 errors, 0 skipped**. Production standalone JAR `target/shopsphere-1.0.0-SNAPSHOT.jar` (66.6 MB) created.
  - Security audit completed: Identified fallback password committed in historical commit `07f6eaa`; documented safe remediation options (inert password rotation vs git history cleanup before remote push).
- **Completed Features**:
  - **Phase 0 Setup**:
    - Environment inspected (Java 24 runtime, Maven 3.9.6, MySQL 8.0, Git 2.51).
    - Authored master documentation: `AGENTS.md`, `docs/requirements.md`, `docs/architecture.md`, `docs/database-design.md`, `docs/api-reference.md`, `docs/test-plan.md`, `docs/deployment.md`, `docs/implementation-status.md`.
    - Initialized Git repository and baseline Spring Boot 3.4.3 project.
  - **Phase 1 Backend Foundation**:
    - Configured Flyway versioned migrations: `V1__create_categories_and_products.sql` and `V2__seed_categories_and_products.sql`.
    - Created JPA persistence entities `Category` and `Product` with `@PrePersist`/`@PreUpdate`, lifecycle callbacks, and `BigDecimal` monetary precision.
    - Implemented `CategoryRepository` and `ProductRepository` extending `JpaRepository` and `JpaSpecificationExecutor<Product>`.
    - Built comprehensive DTO layer (`CategoryRequestDTO`, `CategoryResponseDTO`, `ProductRequestDTO`, `ProductResponseDTO`, `ApiResponse<T>`, `PageResponse<T>`).
    - Implemented business services `CategoryService` and `ProductService` with transactional boundaries, auto slug generation, safe pagination, and multi-field sorting.
    - Implemented REST controllers `CategoryController` and `ProductController` with OpenAPI annotations.
    - Centralized error handling via `@RestControllerAdvice` (`GlobalExceptionHandler`).
  - **Phase 2 Authentication & Security**:
    - Applied Flyway migration `V3__create_users_and_addresses.sql` on MySQL creating normalized `users` and `addresses` tables and seeding dev admin account.
    - Implemented `User`, `Role`, and `Address` JPA entities.
    - Implemented `UserRepository` (case-insensitive email lookup) and `AddressRepository`.
    - Implemented `UserPrincipal` and `CustomUserDetailsService`.
    - Configured Spring Security 6 / Spring Boot 3.4 in `SecurityConfig`:
      - BCrypt password encoder (strength 12).
      - Session-based authentication with session fixation protection (`migrateSession()`).
      - CSRF protection via `CookieCsrfTokenRepository.withHttpOnlyFalse()`.
      - Role-based authorization (`ROLE_CUSTOMER`, `ROLE_ADMIN`). Public catalog browsing, Admin-only product/category mutations.
      - JSON-based `AuthenticationEntryPoint` (401) and `AccessDeniedHandler` (403).
    - Implemented `AuthService` and `AuthServiceImpl` with defensive role assignment (strictly `ROLE_CUSTOMER` on registration).
    - Implemented `AuthController` (`/api/auth/register`, `/api/auth/login`, `/api/auth/logout`, `/api/auth/me`, `/api/auth/csrf`).
  - **Phase 3 Product Catalog & Frontend Integration**:
    - Enhanced `ProductServiceImpl`: Added defensive validation for price boundaries (`minPrice >= 0`, `maxPrice >= 0`, `minPrice <= maxPrice` returning 400 Bad Request).
    - Enhanced `ProductSpecification`: Added explicit Left Joins on `category` and `parent` so parent category queries retrieve products from all nested subcategories.
    - Configured `SecurityConfig`: Permitted public access to `/product.html` while strictly maintaining admin protections on mutating catalog endpoints.
    - Authored responsive, accessible Vanilla JS frontend:
      - `src/main/resources/static/index.html`: Storefront homepage with dynamic category pills, search bar, price filters, in-stock toggle, sorting dropdown, products grid, active filter chips, skeleton shimmer loaders, empty state, and pagination controls.
      - `src/main/resources/static/product.html`: Dedicated deep-linkable Product Details page with breadcrumbs, large product image, full description, quantity selector with boundary validation, low-stock warning banners, and technical specifications table.
      - `src/main/resources/static/js/catalog.js`: ES6 catalog manager with 350ms debounced search, multi-facet filtering, URL query parameter synchronization, and interactive Quick-View modal.
      - `src/main/resources/static/js/product-detail.js`: ES6 product details manager with dynamic retrieval, quantity stepper, and simulated cart interaction.
      - `src/main/resources/static/css/styles.css`: Complete responsive design system with modal, skeleton loading, filter toolbar, pagination, and breadcrumb styles.
  - **Phase 4 Shopping Cart, Checkout Simulation & Order Management**:
    - **Database Migration**: Created `V4__create_cart_and_orders.sql` on MySQL creating normalized `carts`, `cart_items` (unique constraint on `cart_id, product_id`), `orders`, `order_items`, `payment_attempts`, and `inventory_audit_log` tables with foreign keys and performance indexes.
    - **Concurrency-Safe Pessimistic Locking**: Configured `@Lock(LockModeType.PESSIMISTIC_WRITE)` query methods in `ProductRepository` (`SELECT ... FOR UPDATE`), ensuring thread-safe inventory deduction and total prevention of negative stock under race conditions.
    - **Persistent Shopping Cart**: Linked to authenticated customer with strict cross-customer isolation. Server-side automatic calculation of unit prices, line totals, 5% tax, and conditional shipping ($9.99 or FREE over $100.00). Client prices are never trusted.
    - **Transactional Checkout Service**: Annotated with `@Transactional(rollbackFor = Exception.class)`. Performs duplicate submission prevention via UUID `idempotencyKey`, pessimistic locking of all cart products, stock sufficiency validation, creation of immutable item snapshots, inventory reduction with audit log tracking, simulated payment processing, and automatic cart clearing.
    - **Simulated Payment Gateway**: Supports `SIMULATED_CARD`, `SIMULATED_UPI`, and `SIMULATED_NET_BANKING` with explicit development simulation outcomes (`SUCCESS` or `FAILED`). On simulated failure, throws `PaymentFailedException`, executing a full transactional rollback where inventory remains untouched and the cart remains intact.
    - **Immutable Historical Snapshots**: `OrderItem` stores immutable copies of `productNameSnapshot`, `productSkuSnapshot`, `unitPriceSnapshot`, and `unitCostSnapshot`. Future price changes or product deletions do not corrupt historical invoices or customer receipts.
    - **Customer Order Tracking & Privacy**: Implemented `/api/orders/my`, `/api/orders/{id}`, `/api/orders/number/{orderNumber}` ensuring customers can only view their own orders.
    - **Responsive Frontend Modules**:
      - `src/main/resources/static/cart.html` & `src/main/resources/static/js/cart.js`: Cart viewer with quantity stepper, item removal, cart clearing, and live summary.
      - `src/main/resources/static/checkout.html` & `src/main/resources/static/js/checkout.js`: Checkout flow with saved address radio selection, new address form, payment method selector, simulation outcome toggle (for live interview ACID demonstration), and idempotency key generation.
      - `src/main/resources/static/order-confirmation.html` & `src/main/resources/static/js/order-confirmation.js`: Customer receipt page displaying order confirmation, shipping destination, financial totals, and immutable snapshot table.
      - `src/main/resources/static/orders.html` & `src/main/resources/static/js/orders.js`: Customer order history table with status badges (`CONFIRMED`, `PAID`, `PENDING`, etc.), pagination, and quick-view detail modal.
      - `src/main/resources/static/js/auth.js`: Real-time cart item count badge on navbar and conditional "Admin Console" link for `ROLE_ADMIN` users.
  - **Phase 5 Admin Dashboard, Inventory Management & Business Analytics**:
    - **Database Migration**: Created `V5__admin_inventory_and_analytics_enhancements.sql` adding `unit_cost_snapshot` to `order_items` (backfilled from `products.cost_price`), `stock_before` and `user_id` FK to `inventory_audit_log`, and composite performance indexes (`idx_orders_status_created`, `idx_inventory_audit_created`, `idx_inventory_audit_movement`).
    - **Historical Gross Profit Accounting**: Populates `order_items.unit_cost_snapshot` at checkout time using `product.getCostPrice()`. Calculates estimated gross profit deterministically as $\sum (\text{lineTotal} - (\text{unitCostSnapshot} \times \text{quantity}))$, insulating historical financials from subsequent product cost changes.
    - **Order Lifecycle State Machine**: Enforces strict state transitions (`PENDING` -> `CONFIRMED` -> `SHIPPED` -> `DELIVERED`, and cancellation from `PENDING`/`CONFIRMED`). On cancellation, automatically restores inventory using `@Lock(LockModeType.PESSIMISTIC_WRITE)` and records an `ORDER_CANCELLED_RESTORE` audit log.
    - **Continuous Time-Series Sales Analytics**: Generates unbroken daily revenue, profit, and order volume timeline covering the requested date range, initializing zero-value placeholder days to prevent chart skew.
    - **Warehouse Asset Valuation**: Aggregates total warehouse investment at cost, total retail asset value, unrealized gross profit, and total units in stock.
    - **Concurrency-Safe Restocking & Adjustments**: Pessimistic write locks ensure safe concurrent restocks and manual stock adjustments. Negative adjustments exceeding available stock are rejected with `BadRequestException`, and a justification reason is strictly mandatory.
    - **Role-Based Security Discipline**: All `/api/admin/**` endpoints are strictly guarded by Spring Security requiring `ROLE_ADMIN`. Anonymous users receive 401 Unauthorized, and customer users (`ROLE_CUSTOMER`) receive 403 Forbidden.
    - **Responsive Admin Portal**:
      - `src/main/resources/static/admin.html`: Tabbed portal with Overview (KPI cards + interactive daily sales bar chart), Inventory Management (search, filter, stock badges, restock/adjust/threshold modals, movement history), Order Fulfillment (order search, status badges, details modal, status update workflow), and Reports & Valuation.
      - `src/main/resources/static/js/admin.js`: Vanilla ES6 script managing role checks, dynamic charts, modals, and API calls.
      - `src/main/resources/static/css/styles.css`: Complete styling for KPI cards, tab navigation, admin tables, stock badges, and CSS bar charts.
  - **Phase 6 Comprehensive Testing, Security Audit & Production Readiness**:
    - **Spring Boot Actuator Integration**: Added `spring-boot-starter-actuator` to `pom.xml`. Configured public health monitoring at `/actuator/health` and `/actuator/info` for cloud load balancers and deployment liveness probes. Sensitive administrative actuators are protected and require `ROLE_ADMIN`.
    - **Security Headers Hardening**: Configured strict security headers in `SecurityConfig.java`:
      - `X-Content-Type-Options: nosniff` (MIME-sniffing prevention)
      - `X-Frame-Options: DENY` (Clickjacking prevention)
      - `Referrer-Policy: strict-origin-when-cross-origin` (Information leakage prevention)
      - `Content-Security-Policy: default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self'; connect-src 'self'` (Cross-site scripting mitigation)
    - **Defensive Error Handling**:
      - Added `HttpMessageNotReadableException` handler in `GlobalExceptionHandler` to safely intercept malformed JSON bodies, returning 400 Bad Request with a clean message and preventing internal Jackson parser stack traces from leaking.
      - Added `IllegalArgumentException` and `IllegalStateException` handlers returning 400 Bad Request.
    - **Date Range Defensive Fix**:
      - Hardened `AdminDashboardServiceImpl.resolveDateRange` to ensure `resolvedStart` is never after `resolvedEnd` under any parameter combination (future dates, missing end date, inverted ranges), throwing 400 Bad Request.
    - **Standalone Production Packaging**:
      - Verified `mvn clean package -DskipTests=false` builds an executable, standalone Spring Boot fat JAR (`shopsphere-1.0.0-SNAPSHOT.jar`, ~66.6 MB).
      - Configured separate runtime profiles (`application-dev.yml` and `application-prod.yml`) with environment variable bindings documented in `.env.example`.
- **Known Defects**: None.
- **Tests Executed & Actual Results**:
  - `mvn clean test`: **BUILD SUCCESS**
  - Total Tests Run: **144**, Failures: **0**, Errors: **0**, Skipped: **0**
    - `ShopSphereApplicationTests`: 1 passed (Spring context boot)
    - `CategoryRepositoryTest`: 5 passed (JPA persistence & queries)
    - `ProductRepositoryTest`: 6 passed (Category hierarchy, keyword query, price boundaries, stock filtering)
    - `UserRepositoryTest`: 2 passed (Email normalization & lookup)
    - `PasswordEncoderTest`: 1 passed (BCrypt strength 12 verification)
    - `CategoryServiceTest`: 5 passed (Mockito unit tests)
    - `ProductServiceTest`: 8 passed (Mockito unit tests + price boundary validations)
    - `AuthServiceTest`: 4 passed (Mockito unit tests for register & login)
    - `CartServiceTest`: 7 passed (Add item, stock validation, recalculations, update quantity, remove item, clear cart, user isolation)
    - `OrderServiceTest`: 6 passed (Successful checkout, rollback on payment failure, out of stock rejection, idempotency key replay, user isolation)
    - `OrderConcurrencyIntegrationTest`: 1 passed (Pessimistic locking stress test: concurrent checkout for the final unit of stock—exactly 1 succeeds, 1 fails with InsufficientStockException, final stock is 0 without overselling)
    - `AdminInventoryServiceTest`: 9 passed (Restocking, positive/negative adjustments, negative stock guard, mandatory reasons, threshold updates)
    - `AdminOrderServiceTest`: 8 passed (Fulfillment lifecycle, cancellation stock restoration, terminal state protection, gross profit calculation)
    - `AdminDashboardServiceTest`: 7 passed (+3 Phase 6 edge cases: inverted date range rejection, future start date rejection, zero-order null-safety & AOV zero-division protection)
    - `AdminSecurityAuthorizationTest`: 16 passed (Anonymous 401, customer 403, admin 200, CSRF protection across all admin endpoints)
    - `CategoryControllerTest`: 7 passed (MockMvc REST API & validation tests)
    - `ProductControllerTest`: 12 passed (MockMvc REST API, pagination, filtering, query param validation, 400/404 handling)
    - `AuthControllerTest`: 8 passed (MockMvc auth endpoints & validation)
    - `SecurityAuthorizationTest`: 8 passed (Role access control, public browsing, CSRF enforcement)
    - `CartControllerTest`: 6 passed (MockMvc cart endpoints, authenticated security, validation)
    - `OrderControllerTest`: 4 passed (MockMvc checkout, order retrieval, security enforcement)
    - `ActuatorAndSecurityHeadersTest`: 5 passed (New in Phase 6: `/actuator/health` 200 UP, security headers verification, unauthenticated actuator 401, customer actuator 403, admin actuator 200)
    - `GlobalExceptionHandlerTest`: 6 passed (New in Phase 6: malformed JSON 400, path variable type mismatch 400, negative price filter 400, inverted price filter 400, registration validation field errors 400, admin date inversion 400)
    - `WorkflowEndToEndIntegrationTest`: 2 passed (New in Phase 6: full business workflow add-to-cart -> checkout -> admin fulfillment lifecycle -> analytics, and order cancellation inventory restoration + audit log + analytics exclusion)
- **Production Readiness Check**:
  - Executable JAR verified: `target/shopsphere-1.0.0-SNAPSHOT.jar`
  - Health check endpoint verified: `GET /actuator/health`
  - Production security verified: session cookie secure flag enabled in prod, CSRF enabled, BCrypt 12, CSP and Clickjacking defense configured.
  - Multi-stage Dockerfile verified with non-root user.
  - Standalone JAR verified running locally on port 8089 with MySQL.
- **Outstanding Tasks**:
  - Await user approval before provisioning any live cloud databases or executing external deployments.
- **Next Recommended Action**:
  - Present Phase 7 Final Report covering deployment readiness, hosting comparison, and resume portfolio launch. Await explicit approval before deploying.

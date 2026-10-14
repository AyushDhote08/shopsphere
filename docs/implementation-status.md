# ShopSphere — Implementation Status & Phasing Tracker

## 1. Project Phase Roadmap

| Phase | Description | Status | Completion Target |
|---|---|---|---|
| **Phase 0** | Requirements, Environment, Architecture, Git Setup, Project Baseline | **Completed** | Baseline verified & tested |
| **Phase 1** | Database Migrations & Backend Foundation (Catalog, Categories, DTOs, APIs) | **Completed** | 36/36 tests passing & MySQL verified |
| **Phase 2** | Authentication & Security (Spring Security, Sessions, BCrypt, Roles, CSRF, Auth UI) | **Completed** | 59/59 tests passing & live session verified |
| **Phase 3** | Product Catalog & Categories (Frontend UI & Storefront Integration) | **Completed** | 67/67 tests passing & live storefront verified |
| **Phase 4** | Shopping Cart, Checkout Simulation, and Order Management | **Completed** | Persistent Cart + Pessimistic Locking + 91/91 tests passing |
| **Phase 5** | Admin Dashboard & Business Analytics | Planned | KPIs + Reports + Stock Alerts |
| **Phase 6** | Reviews, Wishlist & Customer Extras | Planned | Ratings + Wishlist |
| **Phase 7** | Comprehensive Testing, Security Audit & Hardening | Planned | Negative tests + Concurrency stress tests |
| **Phase 8** | Production Deployment, Interview Guide & Documentation | Planned | Docker + Live Demo |

---

## 2. Current Status Summary

- **Current Active Phase**: **Phase 4 Completed** (Shopping Cart, Server-Side Recalculations, Checkout Simulation, Pessimistic Row Locking, Immutable Order Snapshots, Idempotency, Order History & Receipt UI).
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
    - **Persistent Shopping Cart**: Linked to authenticated customer with strict cross-customer isolation. Server-side automatic calculation of unit prices, line totals, 5% tax, and conditional shipping ($15.00 or FREE over $100.00). Client prices are never trusted.
    - **Transactional Checkout Service**: Annotated with `@Transactional(rollbackFor = Exception.class)`. Performs duplicate submission prevention via UUID `idempotencyKey`, pessimistic locking of all cart products, stock sufficiency validation, creation of immutable item snapshots, inventory reduction with audit log tracking, simulated payment processing, and automatic cart clearing.
    - **Simulated Payment Gateway**: Supports `SIMULATED_CARD`, `SIMULATED_UPI`, and `SIMULATED_NET_BANKING` with explicit development simulation outcomes (`SUCCESS` or `FAILED`). On simulated failure, throws `PaymentFailedException`, executing a full transactional rollback where inventory remains untouched and the cart remains intact.
    - **Immutable Historical Snapshots**: `OrderItem` stores immutable copies of `productNameSnapshot`, `productSkuSnapshot`, and `unitPriceSnapshot`. Future price changes or product deletions do not corrupt historical invoices or customer receipts.
    - **Customer Order Tracking & Privacy**: Implemented `/api/orders/my`, `/api/orders/{id}`, `/api/orders/number/{orderNumber}` ensuring customers can only view their own orders.
    - **Responsive Frontend Modules**:
      - `src/main/resources/static/cart.html` & `src/main/resources/static/js/cart.js`: Cart viewer with quantity stepper, item removal, cart clearing, and live summary.
      - `src/main/resources/static/checkout.html` & `src/main/resources/static/js/checkout.js`: Checkout flow with saved address radio selection, new address form, payment method selector, simulation outcome toggle (for live interview ACID demonstration), and idempotency key generation.
      - `src/main/resources/static/order-confirmation.html` & `src/main/resources/static/js/order-confirmation.js`: Customer receipt page displaying order confirmation, shipping destination, financial totals, and immutable snapshot table.
      - `src/main/resources/static/orders.html` & `src/main/resources/static/js/orders.js`: Customer order history table with status badges (`CONFIRMED`, `PAID`, `PENDING`, etc.), pagination, and quick-view detail modal.
      - `src/main/resources/static/js/auth.js`: Real-time cart item count badge on navbar.
- **Known Defects**: None.
- **Tests Executed & Actual Results**:
  - `mvn clean test`: **BUILD SUCCESS**
  - Total Tests Run: **91**, Failures: **0**, Errors: **0**, Skipped: **0**
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
    - `CategoryControllerTest`: 7 passed (MockMvc REST API & validation tests)
    - `ProductControllerTest`: 12 passed (MockMvc REST API, pagination, filtering, query param validation, 400/404 handling)
    - `AuthControllerTest`: 8 passed (MockMvc auth endpoints & validation)
    - `SecurityAuthorizationTest`: 8 passed (Role access control, public browsing, CSRF enforcement)
    - `CartControllerTest`: 6 passed (MockMvc cart endpoints, authenticated security, validation)
    - `OrderControllerTest`: 4 passed (MockMvc checkout, order retrieval, security enforcement)
- **Manual Live Verification**:
  - Verified static endpoints `/cart.html`, `/checkout.html`, `/order-confirmation.html`, and `/orders.html` are accessible.
  - Verified authenticated endpoints `/api/cart/**`, `/api/orders/**`, `/api/addresses/**` reject unauthenticated requests with 401 Unauthorized.
  - Verified multi-threaded pessimistic locking ensures concurrency safety on contested stock.
- **Outstanding Tasks**:
  - Await user review and approval of Phase 4 deliverables.
- **Next Recommended Action**:
  - Proceed to Phase 5: Admin Dashboard, Inventory Restocking, and Business Analytics.

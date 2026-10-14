# ShopSphere — Requirements & Acceptance Criteria Checklist

## 1. Project Overview
ShopSphere is a robust, production-grade e-commerce and inventory management platform built with Spring Boot, Spring Security, Spring Data JPA, MySQL, and modern Vanilla JavaScript / CSS3. It is designed to demonstrate deep full-stack proficiency, transaction safety, clean layered design, and enterprise development standards.

---

## 2. Functional Requirements Checklist

### Module A: Customer Storefront
- [ ] **Home & Navigation**:
  - Responsive header with search bar, category navigation, cart badge with live item count, user auth controls.
  - Featured products carousel/grid and promotional banners.
- [ ] **Product Catalog & Browsing**:
  - Full-text or partial keyword product search.
  - Category, price range, and stock-status filters.
  - Sorting: Price (Low to High / High to Low), Newest Arrivals, Customer Rating.
  - Server-side pagination with page sizes and page navigation controls.
- [ ] **Product Details**:
  - High-res image display, SKU, category, brand, live stock availability indicator.
  - Server-calculated price, discounts, and detailed descriptions.
  - Customer reviews list and review submission form (validated for verified buyers).
- [ ] **Customer Account & Security**:
  - Registration with duplicate email/username validation and password policy enforcement.
  - Secure login (BCrypt authentication), session or token management, and logout.
  - Customer profile update and shipping/billing address book management (multi-address support with default flag).
- [ ] **Persistent Shopping Cart & Wishlist**:
  - Persistent cart stored in the database for authenticated users.
  - Dynamic quantity update, stock threshold validation, and item removal.
  - Subtotal, tax, and order total recalculation performed strictly on the backend.
  - Customer wishlist with one-click "Move to Cart".
- [ ] **Checkout & Order Placement**:
  - Delivery address selection or inline address creation.
  - Order summary breakdown (items, unit prices, subtotal, shipping, taxes, total).
  - Concurrency-safe stock deduction during order checkout.
  - Order confirmation screen with tracking number, order status, and itemized invoice receipt.
- [ ] **Order History & Tracking**:
  - Customer order list with status badges (`PENDING`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED`).
  - Order cancellation option for eligible statuses (`PENDING`, `CONFIRMED`), triggering automatic stock restoration.

---

### Module B: Administrator Dashboard
- [ ] **Executive Analytics & KPI Cards**:
  - Real-time KPIs calculated directly from the database: Total Revenue, Gross Profit, Total Orders, Active Customers, Low-Stock Alert Count.
  - Recent orders table with fast status update actions.
- [ ] **Product & Catalog Management**:
  - Product CRUD with SKU uniqueness enforcement, category assignment, cost price, selling price, and stock levels.
  - Soft deactivation (`isActive = false`) to preserve historical order integrity without foreign key violations.
  - Low-stock inventory table with visual highlight (items where `stockQuantity <= lowStockThreshold`).
- [ ] **Category Management**:
  - Add, edit, deactivate categories with slug generation and hierarchical or flat structure.
- [ ] **Order Processing & Fulfillment**:
  - Filter orders by status, customer, date range.
  - Enforced state machine transitions (e.g., `PENDING` → `CONFIRMED` → `SHIPPED` → `DELIVERED`, or `CANCELLED`).
  - Automated stock restoration upon order cancellation.
- [ ] **Customer Management**:
  - View registered customers, account status, total orders placed, and lifetime spend.
  - Ability to deactivate/suspend accounts.
- [ ] **Inventory Movement Tracking**:
  - Explicit audit ledger recording stock additions, deductions (orders), and restorations (cancellations).
- [ ] **Sales & Profit Reports**:
  - Date-filtered revenue and profit breakdown (Revenue - Cost of Goods Sold = Gross Margin).
  - Top-selling products and top-performing categories.

---

### Module C: E-commerce Business Logic & Safety Rules
- [ ] **Server-Side Trust Boundary**:
  - Client sends only item IDs and desired quantities. All prices, totals, tax, and stock availability are resolved server-side.
- [ ] **Transactional Atomicity**:
  - Placing an order must either completely succeed (record order, record items, deduct stock, create payment record, clear cart) or roll back cleanly without leaving orphan records or corrupted stock counts.
- [ ] **Concurrency-Safe Stock Updates**:
  - Stock deductions execute under database concurrency protection (e.g., `PESSIMISTIC_WRITE` locks or atomic update queries `stock = stock - :qty WHERE id = :id AND stock >= :qty`) preventing race conditions (overselling).
- [ ] **Simulated Payment Gateway**:
  - Clean abstraction for payment processing supporting status outcomes: `SUCCESS`, `FAILED`, `CANCELLED`.
  - No real credit card information collected or stored.
- [ ] **Duplicate Order Prevention**:
  - Idempotency check / order submission protection preventing double-charging or repeated deductions from rapid double-clicks.

---

## 3. Non-Functional & Technical Acceptance Criteria

| Criteria | Specification | Verification Method |
|---|---|---|
| **Architecture** | Layered `Controller` → `Service` → `Repository` → `Database` | Code review and structural checks |
| **DTO Usage** | No entity exposure on HTTP endpoints | Controller method signature review |
| **Precision** | All currency stored as `DECIMAL(12,2)` and handled as `BigDecimal` | Entity schemas, DTOs, calculations |
| **Security** | Spring Security with BCrypt, role-based checks, CSRF protection, Customer data isolation | Automated security integration tests |
| **Error Handling** | Uniform JSON error responses with HTTP 400, 401, 403, 404, 409, 500 | `@RestControllerAdvice` tests |
| **Testing** | Unit tests (Service layer with Mockito) & Integration tests (MockMvc + MySQL/H2) | Maven test run with zero failures |
| **API Docs** | Interactive OpenAPI / Swagger UI at `/swagger-ui.html` | Visual check and endpoint contract tests |
| **Zero Mock Data** | All UI components bound to live Spring Boot REST endpoints | End-to-end browser and API validation |

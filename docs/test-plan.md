# ShopSphere — Master Test Plan & Quality Assurance Strategy

## 1. Testing Strategy Overview

Quality assurance for ShopSphere follows the automated testing pyramid:
1. **Unit Tests (Mockito + JUnit 5)**: Rapid verification of isolated business logic, calculations, discount formulas, and edge-case exceptions.
2. **Integration Tests (MockMvc + SpringBootTest)**: Verification of HTTP request handling, DTO deserialization, Bean Validation constraints, Spring Security filters, and transactional integrity.
3. **Database Repository Tests (`@DataJpaTest`)**: Verification of custom JPQL queries, pessimistic locking semantics, and relational constraints.
4. **End-to-End Functional Verification (`@SpringBootTest`)**: Automated integration tests verifying full multi-role user workflows (Register → Login → Add to Cart → Checkout → Deduct Stock → Admin Fulfillment → Analytics Calculation → Cancellation & Inventory Restoration).
5. **Production Readiness & Actuator Tests**: Verification of health endpoints (`/actuator/health`), HTTP security headers (CSP, Clickjacking, MIME sniffing, Referrer-Policy), and sensitive actuator protection.

---

## 2. Test Execution Matrix & Automated Results

**Total Tests Run**: **144** | **Failures**: **0** | **Errors**: **0** | **Skipped**: **0** | **Result**: **BUILD SUCCESS**

### 2.1 Test Suite Breakdown

| Category | Test Class | Test Count | Status | Key Coverage |
|---|---|---|---|---|
| **Context** | `ShopSphereApplicationTests` | 1 | Passed | Spring Boot application context boot |
| **Persistence** | `CategoryRepositoryTest` | 5 | Passed | JPA persistence, active categories, hierarchy |
| **Persistence** | `ProductRepositoryTest` | 6 | Passed | Multi-facet filter, keyword search, price boundaries, stock filtering |
| **Persistence** | `UserRepositoryTest` | 2 | Passed | Case-insensitive email query, role mapping |
| **Security** | `PasswordEncoderTest` | 1 | Passed | BCrypt strength 12 verification |
| **Security** | `SecurityAuthorizationTest` | 8 | Passed | Role access control, public browsing, CSRF enforcement |
| **Security** | `AdminSecurityAuthorizationTest` | 16 | Passed | Anonymous 401, customer 403, admin 200, CSRF on admin APIs |
| **Security & Ops** | `ActuatorAndSecurityHeadersTest` | 5 | Passed | `/actuator/health` 200 UP, CSP, X-Frame-Options, MIME sniffing, role check |
| **Service Logic** | `CategoryServiceTest` | 5 | Passed | Mockito unit tests for category CRUD |
| **Service Logic** | `ProductServiceTest` | 8 | Passed | Mockito unit tests + price boundary validations |
| **Service Logic** | `AuthServiceTest` | 4 | Passed | Registration validation, BCrypt password hashing, session login |
| **Service Logic** | `CartServiceTest` | 7 | Passed | Cart item add, max item limit, stock validation, cross-customer isolation |
| **Service Logic** | `OrderServiceTest` | 6 | Passed | Checkout, rollback on payment failure, out of stock, idempotency replay |
| **Service Logic** | `AdminInventoryServiceTest` | 9 | Passed | Restock, stock adjustments, negative stock guard, mandatory reasons |
| **Service Logic** | `AdminOrderServiceTest` | 8 | Passed | Fulfillment lifecycle, cancellation inventory restoration, gross profit snapshot |
| **Service Logic** | `AdminDashboardServiceTest` | 7 | Passed | KPI calculation, continuous daily sales timeline, zero-order safety, date inversion |
| **REST Controller** | `CategoryControllerTest` | 7 | Passed | MockMvc category REST API & validation |
| **REST Controller** | `ProductControllerTest` | 12 | Passed | MockMvc product pagination, filter params, 400/404 handling |
| **REST Controller** | `AuthControllerTest` | 8 | Passed | MockMvc registration, login, logout, current user, CSRF provider |
| **REST Controller** | `CartControllerTest` | 6 | Passed | MockMvc cart endpoints, authenticated security, input validation |
| **REST Controller** | `OrderControllerTest` | 4 | Passed | MockMvc checkout, order retrieval, security enforcement |
| **Exception Handling**| `GlobalExceptionHandlerTest` | 6 | Passed | Malformed JSON 400, type mismatch 400, price inversion 400, field errors |
| **Concurrency Stress**| `OrderConcurrencyIntegrationTest` | 1 | Passed | Pessimistic locking stress test: simultaneous checkout for last unit of stock |
| **End-to-End E2E** | `WorkflowEndToEndIntegrationTest` | 2 | Passed | Complete customer checkout to admin fulfillment + cancellation inventory restore |

---

## 3. Critical Negative & Edge-Case Test Scenarios

| Test ID | Scenario Description | Expected HTTP Status / Exception | Key Verification Point |
|---|---|---|---|
| **NEG-01** | Unauthenticated user calls protected endpoint (`GET /api/cart`) | `401 Unauthorized` | Returns structured JSON `ErrorResponse` (no login redirect for APIs) |
| **NEG-02** | Customer calls Admin endpoint (`GET /api/admin/dashboard`) | `403 Forbidden` | Access denied for non-admin role (`ROLE_CUSTOMER`) |
| **NEG-03** | Customer attempts to inspect another customer's order ID | `404 Not Found` | Data isolation: Query filtered strictly by authenticated `userId` |
| **NEG-04** | Registration with duplicate email address | `409 Conflict` | Unique constraint violation trapped gracefully with `DuplicateResourceException` |
| **NEG-05** | Login with incorrect password | `401 Unauthorized` | `BadCredentialsException` handled, no internal trace leaked |
| **NEG-06** | Add to cart with negative or zero quantity | `400 Bad Request` | Jakarta Bean Validation / service check rejects with field error |
| **NEG-07** | Checkout when cart quantity exceeds product inventory | `400 Bad Request` | `InsufficientStockException`, transaction rolled back |
| **NEG-08** | Concurrent checkout for last available inventory unit | Exactly 1 `200 OK`, exactly 1 `400 Bad Request` | Pessimistic locking prevents double-deduction (stock is 0, never -1) |
| **NEG-09** | Payment simulation returns `FAILED` | `400 Bad Request` | `PaymentFailedException` rolls back transaction: inventory untouched, cart intact |
| **NEG-10** | Attempt illegal order status transition (e.g., `DELIVERED` → `CANCELLED`) | `400 Bad Request` | State machine validation rejects invalid transition |
| **NEG-11** | Customer sends fraudulent price or total in checkout payload | Client price ignored | Server recalculates prices strictly from persisted database products |
| **NEG-12** | Client sends malformed JSON payload | `400 Bad Request` | `HttpMessageNotReadableException` handled without leaking Jackson class details |
| **NEG-13** | Inverted price filter (`minPrice > maxPrice`) | `400 Bad Request` | Defensive price validation rejects inverted range |
| **NEG-14** | Admin dashboard inverted date range (`startDate > endDate`) | `400 Bad Request` | Defensive date resolution rejects inverted range |

---

## 4. Concurrency Test Specification (Race Condition Prevention)

### Scenario: Race Condition on Last Item in Stock
- **Setup**: Product has `stock_quantity = 1`.
- **Action**: Two concurrent threads execute checkout simultaneously requesting 1 unit of the product.
- **Verification**:
  - Thread A acquires `PESSIMISTIC_WRITE` lock (`SELECT ... FOR UPDATE`), decrements stock to 0, completes order.
  - Thread B acquires lock next, evaluates stock = 0, throws `InsufficientStockException`.
  - Database assertion: `stock_quantity == 0` (never -1). Exactly one order is created.

---

## 5. Test Execution Commands

```bash
# Run all 144 unit, integration, and E2E tests
mvn clean test

# Run a specific test class
mvn test -Dtest=WorkflowEndToEndIntegrationTest

# Run all security integration tests
mvn test -Dtest=*SecurityTest

# Run packaging with full test execution
mvn clean package -DskipTests=false
```

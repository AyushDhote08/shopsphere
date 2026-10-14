# ShopSphere — Master Test Plan & Quality Assurance Strategy

## 1. Testing Strategy Overview

Quality assurance for ShopSphere follows the testing pyramid:
1. **Unit Tests (Mockito + JUnit 5)**: Rapid verification of isolated business logic, calculations, discount formulas, and edge-case exceptions.
2. **Integration Tests (MockMvc + SpringBootTest)**: Verification of HTTP request handling, DTO deserialization, Bean Validation constraints, Spring Security filters, and transactional integrity.
3. **Database Repository Tests (`@DataJpaTest`)**: Verification of custom JPQL queries, pessimistic locking semantics, and relational constraints.
4. **End-to-End Functional Verification**: Automated integration scripts testing multi-step user workflows (Register → Login → Add to Cart → Checkout → Deduct Stock → Verify Admin Ledger).

---

## 2. Test Execution Matrix

### 2.1 Critical Negative & Edge-Case Test Scenarios

| Test ID | Scenario Description | Expected HTTP Status / Exception | Key Verification Point |
|---|---|---|---|
| **NEG-01** | Unauthenticated user calls protected endpoint (`GET /api/cart`) | `401 Unauthorized` | Spring Security redirects or yields standard JSON error |
| **NEG-02** | Customer calls Admin endpoint (`GET /api/admin/metrics/overview`) | `403 Forbidden` | Access denied for non-admin role |
| **NEG-03** | Customer attempts to inspect another customer's order ID | `404 Not Found` or `403 Forbidden` | Data isolation: Query filtered strictly by authenticated `userId` |
| **NEG-04** | Registration with duplicate email or duplicate username | `409 Conflict` | Unique constraint violation trapped gracefully |
| **NEG-05** | Login with incorrect password | `401 Unauthorized` | `BadCredentialsException` handled, no internal trace leaked |
| **NEG-06** | Add to cart with negative or zero quantity | `400 Bad Request` | Jakarta Bean Validation rejects with field error |
| **NEG-07** | Checkout when cart quantity exceeds product inventory | `409 Conflict` | `InsufficientStockException`, transaction rolled back |
| **NEG-08** | Concurrent checkout for last available inventory unit | Exactly 1 `201 Created`, exactly 1 `409 Conflict` | Pessimistic locking prevents double-deduction (no negative stock) |
| **NEG-09** | Payment simulation returns `FAILED` | `400 Bad Request` | Order status becomes `FAILED`, stock is NOT deducted or is restored |
| **NEG-10** | Attempt illegal order status transition (e.g., `DELIVERED` → `PENDING`) | `400 Bad Request` | State machine validation rejects invalid transition |
| **NEG-11** | Customer attempts to cancel an already `SHIPPED` order | `400 Bad Request` | Cancellation only permitted for `PENDING` / `CONFIRMED` |
| **NEG-12** | Client sends fraudulent price or total in checkout payload | Price ignored | Server calculates prices strictly from persisted database products |

---

## 3. Concurrency Test Specification (Race Condition Prevention)

### Scenario: Race Condition on Last Item in Stock
- **Setup**: Product #101 has `stock_quantity = 1`.
- **Action**: Two concurrent threads execute checkout simultaneously requesting 1 unit of Product #101.
- **Verification**:
  - Thread A acquires `PESSIMISTIC_WRITE` lock, decrements stock to 0, completes order.
  - Thread B acquires lock next, evaluates stock = 0, throws `InsufficientStockException`.
  - Database assertion: `stock_quantity == 0` (never -1). Exactly one order is created.

---

## 4. Test Execution Commands

```bash
# Run all unit and integration tests
mvn clean test

# Run a specific test class
mvn test -Dtest=OrderServiceTest

# Run all security integration tests
mvn test -Dtest=*SecurityTest
```

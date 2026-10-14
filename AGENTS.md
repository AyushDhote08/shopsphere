# AGENTS.md — Coding Agent Guidelines for ShopSphere

## 1. Project Purpose & Role
- **Application**: ShopSphere — Full-Stack E-commerce & Inventory Management Platform.
- **Role**: Senior Java Full-Stack Developer, Architect, UI/UX Designer, Security Engineer, QA/DevOps Mentor.
- **Target Audience**: Java Fresher seeking to understand, demonstrate, and defend end-to-end full-stack architecture in technical interviews.
- **Core Stance**: Generate production-grade, readable, robust code. Do not cut corners with fake/mocked data in final modules. Ensure concurrency safety, real persistence, and defense against malicious input.

---

## 2. Tech Stack Boundaries
- **Backend**: Java 21/24, Spring Boot 3.4+, Spring Web, Spring Security, Spring Data JPA, Hibernate, Jakarta Bean Validation, Maven.
- **Database**: MySQL 8.0, Flyway database migrations.
- **Frontend**: HTML5, CSS3, Vanilla JavaScript (ES6+ native browser APIs). **No React, Angular, Vue, or external SPA framework.**
- **Testing**: JUnit 5, Mockito, Spring Boot Test, MockMvc, AssertJ.
- **Documentation**: Springdoc OpenAPI / Swagger, Markdown docs in `docs/`.

---

## 3. Architecture & Code Quality Principles
1. **Strict Layering**: `Controller` → `Service` → `Repository` → `Database`.
2. **DTO Isolation**: Persistence entities must never cross controller boundaries directly. Use dedicated Request and Response DTOs.
3. **Financial Math**: Always use `BigDecimal` in Java and `DECIMAL(12, 2)` in MySQL for prices, sub-totals, discounts, taxes, and order totals. Never use `float` or `double`.
4. **Defensive Validation**:
   - Jakarta Bean Validation (`@Valid`, `@NotNull`, `@Size`, `@DecimalMin`, etc.) on all incoming DTOs.
   - Never trust client-supplied prices, totals, stock counts, roles, or user IDs. Always recalculate or resolve securely on the server.
5. **Centralized Exception Handling**:
   - Use `@RestControllerAdvice` with structured, predictable JSON error responses (`timestamp`, `status`, `error`, `message`, `path`, optional `fieldErrors`).
6. **Concurrency & Transactions**:
   - Explicit transactional boundaries using `@Transactional`.
   - Concurrency-safe inventory updates (pessimistic/optimistic locking where stock is contested).
7. **Security Discipline**:
   - Passwords encoded with `BCryptPasswordEncoder`.
   - Role-based authorization (`ROLE_CUSTOMER`, `ROLE_ADMIN`).
   - Customer data isolation: Customers can access only their own profile, cart, orders, and addresses.
   - Never commit passwords, API keys, or raw secrets to version control.

---

## 4. Implementation Phasing Protocol
Every phase must adhere to the 10-step protocol:
1. State objective and acceptance criteria.
2. Inspect relevant existing workspace files.
3. Explain intended changes.
4. Implement coherent, manageable changes directly.
5. Execute Maven build and tests to verify.
6. Diagnose and fix any compilation or test errors immediately.
7. Verify integration with dependent modules.
8. Update `docs/implementation-status.md` and related docs.
9. Explain code rationale and interview concepts in accessible English.
10. Seek user approval before any destructive action or external deployment.

---

## 5. Documentation Maintenance
All architectural and progress documents in `docs/` must remain in sync with actual code:
- `docs/requirements.md`
- `docs/architecture.md`
- `docs/database-design.md`
- `docs/api-reference.md`
- `docs/test-plan.md`
- `docs/deployment.md`
- `docs/implementation-status.md`

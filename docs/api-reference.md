# ShopSphere — REST API Reference & Contract Specification

## 1. Standard API Conventions

- **Base URL**: `/api`
- **Content Type**: `application/json; charset=UTF-8`
- **Date/Time Format**: ISO-8601 (`YYYY-MM-DDTHH:mm:ssZ`)
- **Monetary Format**: String or Number formatted to 2 decimal places (e.g., `499.99`)
- **Error Response Schema**:
```json
{
  "timestamp": "2026-10-14T21:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Human readable error description",
  "path": "/api/target-endpoint",
  "fieldErrors": [
    {
      "field": "email",
      "rejectedValue": "invalid-email",
      "message": "Must be a valid email address"
    }
  ]
}
```

---

## 2. Authentication & Current User Endpoints

### 2.1 Register New Customer
- **Endpoint**: `POST /api/auth/register`
- **Auth**: Public (Anonymous)
- **Request Body**:
```json
{
  "fullName": "Jane Doe",
  "username": "janedoe",
  "email": "jane.doe@example.com",
  "password": "StrongPassword123!",
  "phone": "+1-555-0199"
}
```
- **Validation**:
  - `fullName`: `@NotBlank`, max 120 chars
  - `username`: `@NotBlank`, alphanumeric 3-30 chars, must be unique
  - `email`: `@NotBlank`, `@Email`, must be unique
  - `password`: `@NotBlank`, min 8 chars with uppercase, lowercase, number, symbol
- **Response**: `201 Created`
```json
{
  "success": true,
  "message": "Customer registered successfully",
  "data": {
    "id": 1,
    "username": "janedoe",
    "email": "jane.doe@example.com",
    "fullName": "Jane Doe",
    "role": "ROLE_CUSTOMER"
  }
}
```
- **Error Responses**: `400 Bad Request` (validation failed), `409 Conflict` (email or username already exists)

### 2.2 Customer & Admin Login
- **Endpoint**: `POST /api/auth/login`
- **Auth**: Public
- **Request Body**:
```json
{
  "usernameOrEmail": "jane.doe@example.com",
  "password": "StrongPassword123!"
}
```
- **Response**: `200 OK` (Establishes secure HTTP-only JSESSIONID cookie)
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "id": 1,
    "username": "janedoe",
    "email": "jane.doe@example.com",
    "fullName": "Jane Doe",
    "role": "ROLE_CUSTOMER"
  }
}
```
- **Error Responses**: `401 Unauthorized` (Invalid credentials)

### 2.3 Get Current Authenticated User (`/me`)
- **Endpoint**: `GET /api/auth/me`
- **Auth**: Authenticated (`ROLE_CUSTOMER` or `ROLE_ADMIN`)
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "username": "janedoe",
    "email": "jane.doe@example.com",
    "fullName": "Jane Doe",
    "role": "ROLE_CUSTOMER"
  }
}
```
- **Error Responses**: `401 Unauthorized` (Not authenticated)

### 2.4 User Logout
- **Endpoint**: `POST /api/auth/logout`
- **Auth**: Authenticated
- **Response**: `200 OK` (Invalidates session cookie)
```json
{
  "success": true,
  "message": "Logout successful"
}
```

---

## 3. Products & Categories (Storefront)

### 3.1 Browse Products with Filters & Pagination
- **Endpoint**: `GET /api/products`
- **Auth**: Public
- **Query Params**:
  - `page`: default 0
  - `size`: default 12
  - `categoryId`: optional Long
  - `query`: optional search string (matches name, description, SKU)
  - `minPrice`: optional BigDecimal
  - `maxPrice`: optional BigDecimal
  - `inStockOnly`: optional Boolean
  - `sort`: `price,asc`, `price,desc`, `createdAt,desc`, `rating,desc`
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 101,
        "sku": "ELEC-HP-001",
        "name": "Wireless Noise-Cancelling Headphones",
        "slug": "wireless-noise-cancelling-headphones",
        "sellingPrice": 149.99,
        "stockQuantity": 25,
        "imageUrl": "/images/headphones.jpg",
        "categoryName": "Audio",
        "averageRating": 4.8,
        "reviewCount": 12
      }
    ],
    "pageNumber": 0,
    "pageSize": 12,
    "totalElements": 48,
    "totalPages": 4,
    "isLast": false
  }
}
```

### 3.2 Get Product Details by Slug or ID
- **Endpoint**: `GET /api/products/{identifier}` (slug or id)
- **Auth**: Public
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 101,
    "sku": "ELEC-HP-001",
    "name": "Wireless Noise-Cancelling Headphones",
    "slug": "wireless-noise-cancelling-headphones",
    "description": "Studio-grade wireless headphones with 40-hour battery life.",
    "sellingPrice": 149.99,
    "stockQuantity": 25,
    "imageUrl": "/images/headphones.jpg",
    "categoryId": 2,
    "categoryName": "Audio",
    "averageRating": 4.8,
    "reviewCount": 12
  }
}
```
- **Error Responses**: `404 Not Found`

### 3.3 List Active Categories
- **Endpoint**: `GET /api/categories`
- **Auth**: Public
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Electronics",
      "slug": "electronics",
      "description": "Smart devices and accessories",
      "productCount": 35
    }
  ]
}
```

---

## 4. Shopping Cart (`/api/cart`)

### 4.1 Get Customer Cart
- **Endpoint**: `GET /api/cart`
- **Auth**: `ROLE_CUSTOMER`
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": {
    "cartId": 15,
    "items": [
      {
        "cartItemId": 42,
        "productId": 101,
        "productName": "Wireless Noise-Cancelling Headphones",
        "productSku": "ELEC-HP-001",
        "imageUrl": "/images/headphones.jpg",
        "unitPrice": 149.99,
        "quantity": 2,
        "availableStock": 25,
        "lineTotal": 299.98
      }
    ],
    "totalItemCount": 2,
    "subtotalAmount": 299.98,
    "estimatedTax": 24.00,
    "estimatedShipping": 0.00,
    "estimatedTotal": 323.98
  }
}
```

### 4.2 Add Item to Cart
- **Endpoint**: `POST /api/cart/items`
- **Auth**: `ROLE_CUSTOMER`
- **Request Body**:
```json
{
  "productId": 101,
  "quantity": 2
}
```
- **Validation**: `productId` != null, `quantity` > 0
- **Response**: `200 OK` (Updated cart structure)
- **Error Responses**: `400 Bad Request`, `404 Product Not Found`, `409 Insufficient Stock`

### 4.3 Update Cart Item Quantity
- **Endpoint**: `PUT /api/cart/items/{cartItemId}`
- **Auth**: `ROLE_CUSTOMER`
- **Request Body**:
```json
{
  "quantity": 3
}
```
- **Response**: `200 OK` (Updated cart)

### 4.4 Remove Item from Cart
- **Endpoint**: `DELETE /api/cart/items/{cartItemId}`
- **Auth**: `ROLE_CUSTOMER`
- **Response**: `200 OK`

---

## 5. Addresses (`/api/addresses`)

### 5.1 List Customer Addresses
- **Endpoint**: `GET /api/addresses`
- **Auth**: `ROLE_CUSTOMER`
- **Response**: `200 OK`

### 5.2 Create Address
- **Endpoint**: `POST /api/addresses`
- **Auth**: `ROLE_CUSTOMER`
- **Request Body**:
```json
{
  "recipientName": "Jane Doe",
  "phone": "+1-555-0199",
  "streetAddress": "742 Evergreen Terrace",
  "city": "Springfield",
  "state": "Oregon",
  "postalCode": "97477",
  "country": "United States",
  "isDefault": true
}
```
- **Response**: `201 Created`

---

## 6. Checkout & Orders (`/api/orders`)

### 6.1 Place Order (Checkout)
- **Endpoint**: `POST /api/orders/checkout`
- **Auth**: `ROLE_CUSTOMER`
- **Request Body**:
```json
{
  "shippingAddressId": 5,
  "paymentMethod": "SIMULATED_CARD",
  "paymentSimulationMode": "SUCCESS"
}
```
- **Response**: `201 Created`
```json
{
  "success": true,
  "message": "Order placed successfully",
  "data": {
    "orderId": 501,
    "orderNumber": "ORD-20261014-9X8K",
    "orderStatus": "CONFIRMED",
    "paymentStatus": "PAID",
    "subtotalAmount": 299.98,
    "taxAmount": 24.00,
    "shippingFee": 0.00,
    "totalAmount": 323.98,
    "placedAt": "2026-10-14T21:30:00Z",
    "items": [
      {
        "productId": 101,
        "productName": "Wireless Noise-Cancelling Headphones",
        "productSku": "ELEC-HP-001",
        "unitPriceSnapshot": 149.99,
        "quantity": 2,
        "lineTotal": 299.98
      }
    ]
  }
}
```
- **Error Responses**: `400 Bad Request` (Empty cart, invalid address), `409 Conflict` (Insufficient stock during lock)

### 6.2 Customer Order History
- **Endpoint**: `GET /api/orders`
- **Auth**: `ROLE_CUSTOMER`
- **Query Params**: `page`, `size`
- **Response**: `200 OK` (Paginated orders belonging to authenticated customer)

### 6.3 Customer Cancel Order
- **Endpoint**: `POST /api/orders/{orderId}/cancel`
- **Auth**: `ROLE_CUSTOMER` (Own orders only)
- **Request Body**:
```json
{
  "reason": "Changed mind"
}
```
- **Response**: `200 OK` (Order cancelled, stock restored automatically)
- **Error Responses**: `400 Bad Request` (Cannot cancel orders already shipped/delivered)

---

## 7. Administrator Management Endpoints (`/api/admin/**`)

### 7.1 Dashboard Metrics & KPI Overview
- **Endpoint**: `GET /api/admin/metrics/overview`
- **Auth**: `ROLE_ADMIN`
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": {
    "totalRevenue": 48250.75,
    "totalGrossProfit": 19420.30,
    "totalOrders": 324,
    "activeCustomers": 180,
    "lowStockCount": 3,
    "pendingOrdersCount": 8
  }
}
```

### 7.2 Admin Product CRUD
- `GET /api/admin/products`: Paginated list including inactive products and profit margins.
- `POST /api/admin/products`: Create new product (validates unique SKU).
- `PUT /api/admin/products/{id}`: Update product details, prices, stock threshold.
- `PATCH /api/admin/products/{id}/status`: Toggle active status (`isActive = true|false`).

### 7.3 Admin Inventory Stock Adjustment
- **Endpoint**: `POST /api/admin/inventory/adjust`
- **Auth**: `ROLE_ADMIN`
- **Request Body**:
```json
{
  "productId": 101,
  "quantityAdjustment": 50,
  "reason": "Supplier restocking batch #4401"
}
```
- **Response**: `200 OK` (Stock updated & written to `inventory_audit_log`)

### 7.4 Admin Order Fulfillment & Status Update
- **Endpoint**: `PATCH /api/admin/orders/{id}/status`
- **Auth**: `ROLE_ADMIN`
- **Request Body**:
```json
{
  "newStatus": "SHIPPED",
  "note": "Dispatched via FedEx Tracking #9938210"
}
```
- **Response**: `200 OK` (Transitions enforced by state machine)

### 7.5 Sales & Financial Reports
- **Endpoint**: `GET /api/admin/reports/sales`
- **Auth**: `ROLE_ADMIN`
- **Query Params**: `startDate`, `endDate`, `groupBy` (`DAY`, `MONTH`, `CATEGORY`)
- **Response**: `200 OK` (Detailed revenue, cost, profit, order counts)

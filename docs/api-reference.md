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

> [!IMPORTANT]
> All `/api/admin/**` endpoints strictly require `ROLE_ADMIN` authorization and valid CSRF token headers (`X-XSRF-TOKEN`) on mutating methods (`POST`, `PUT`, `PATCH`, `DELETE`). Anonymous requests return `401 Unauthorized`, and customer accounts (`ROLE_CUSTOMER`) receive `403 Forbidden`.

### 7.1 Dashboard Metrics & KPI Overview
- **Endpoint**: `GET /api/admin/dashboard`
- **Auth**: `ROLE_ADMIN`
- **Query Params**: `startDate` (`YYYY-MM-DD`), `endDate` (`YYYY-MM-DD`)
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": {
    "totalProducts": 12,
    "totalCategories": 5,
    "totalCustomers": 14,
    "totalOrders": 1,
    "ordersAwaitingProcessing": 0,
    "lowStockCount": 0,
    "periodRevenue": 1154.98,
    "periodProfit": 459.98,
    "periodOrdersCount": 1,
    "periodStartDate": "2026-09-14",
    "periodEndDate": "2026-10-14"
  }
}
```

### 7.2 Daily Sales Time-Series Timeline
- **Endpoint**: `GET /api/admin/dashboard/sales`
- **Auth**: `ROLE_ADMIN`
- **Query Params**: `startDate` (`YYYY-MM-DD`), `endDate` (`YYYY-MM-DD`)
- **Response**: `200 OK` (Continuous timeline without date gaps)
```json
{
  "success": true,
  "data": {
    "startDate": "2026-09-14",
    "endDate": "2026-10-14",
    "totalRevenue": 1154.98,
    "totalProfit": 459.98,
    "totalOrders": 1,
    "averageOrderValue": 1154.98,
    "dailySales": [
      {
        "date": "2026-10-14",
        "orderCount": 1,
        "revenue": 1154.98,
        "profit": 459.98
      }
    ]
  }
}
```

### 7.3 Top-Selling Products Report
- **Endpoint**: `GET /api/admin/dashboard/top-products`
- **Auth**: `ROLE_ADMIN`
- **Query Params**: `limit` (default: 5), `startDate` (`YYYY-MM-DD`), `endDate` (`YYYY-MM-DD`)
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "productId": 7,
      "productName": "4K UHD UltraWide Curved Monitor 34-inch",
      "productSku": "MON-4K-UW34",
      "categoryName": "Monitors",
      "unitsSold": 2,
      "totalRevenue": 1099.98,
      "totalProfit": 459.98
    }
  ]
}
```

### 7.4 Category Sales & Profitability Report
- **Endpoint**: `GET /api/admin/reports/categories`
- **Auth**: `ROLE_ADMIN`
- **Query Params**: `startDate` (`YYYY-MM-DD`), `endDate` (`YYYY-MM-DD`)
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "categoryId": 2,
      "categoryName": "Monitors",
      "unitsSold": 2,
      "revenue": 1099.98,
      "profit": 459.98
    }
  ]
}
```

### 7.5 Warehouse Inventory Asset Valuation
- **Endpoint**: `GET /api/admin/reports/inventory`
- **Auth**: `ROLE_ADMIN`
- **Response**: `200 OK`
```json
{
  "success": true,
  "data": {
    "totalProductsCount": 12,
    "totalUnitsInStock": 773,
    "totalCostValue": 42245.00,
    "totalRetailValue": 84551.00,
    "potentialGrossProfit": 42306.00,
    "lowStockProductsCount": 0
  }
}
```

### 7.6 Inventory Stock Levels & Search
- **Endpoint**: `GET /api/admin/inventory`
- **Auth**: `ROLE_ADMIN`
- **Query Params**: `page` (default 0), `size` (default 10), `query` (optional search term), `lowStockOnly` (boolean)
- **Response**: `200 OK` (Paginated `PageResponse<ProductResponseDTO>`)

### 7.7 Low-Stock Alert Listing
- **Endpoint**: `GET /api/admin/inventory/low-stock`
- **Auth**: `ROLE_ADMIN`
- **Query Params**: `page` (default 0), `size` (default 10)
- **Response**: `200 OK`

### 7.8 Configure Product Reorder Threshold
- **Endpoint**: `PATCH /api/admin/inventory/products/{id}/threshold`
- **Auth**: `ROLE_ADMIN`
- **Request Body**:
```json
{
  "lowStockThreshold": 8
}
```
- **Response**: `200 OK`

### 7.9 Restock Product Inventory
- **Endpoint**: `POST /api/admin/inventory/restock`
- **Auth**: `ROLE_ADMIN`
- **Request Body**:
```json
{
  "productId": 7,
  "quantity": 15,
  "referenceId": "PO-2026-LIVE-01",
  "reason": "Supplier shipment replenishment batch #1"
}
```
- **Response**: `200 OK`

### 7.10 Manual Stock Adjustment
- **Endpoint**: `POST /api/admin/inventory/adjustments`
- **Auth**: `ROLE_ADMIN`
- **Request Body**:
```json
{
  "productId": 7,
  "quantityAdjustment": -2,
  "reason": "Physical count discrepancy / water damage"
}
```
- **Response**: `200 OK`

### 7.11 Inventory Movement Audit Logs
- **Endpoint**: `GET /api/admin/inventory/movements`
- **Auth**: `ROLE_ADMIN`
- **Query Params**: `productId` (optional), `movementType` (optional), `page` (default 0), `size` (default 10)
- **Response**: `200 OK` (Paginated historical audit trail with stock before/after, reasons, and responsible administrator)

### 7.12 Order Search & Fulfillment Listing
- **Endpoint**: `GET /api/admin/orders`
- **Auth**: `ROLE_ADMIN`
- **Query Params**: `status` (optional `OrderStatus`), `orderNumber` (optional), `startDate`, `endDate`, `page`, `size`
- **Response**: `200 OK` (Paginated `PageResponse<AdminOrderResponseDTO>`)

### 7.13 Admin Order Details & Profit Breakdown
- **Endpoint**: `GET /api/admin/orders/{id}`
- **Auth**: `ROLE_ADMIN`
- **Response**: `200 OK` (Includes line-by-line snapshot costs and calculated gross profit)

### 7.14 Order Status Transition
- **Endpoint**: `PATCH /api/admin/orders/{id}/status`
- **Auth**: `ROLE_ADMIN`
- **Request Body**:
```json
{
  "newStatus": "SHIPPED",
  "reason": "Dispatched via FedEx Tracking #FX99283711"
}
```
- **Validation**: State machine enforces `PENDING` $\to$ `CONFIRMED` $\to$ `SHIPPED` $\to$ `DELIVERED`. If `CANCELLED` from active holding state, automatically restores stock with pessimistic lock.
- **Response**: `200 OK`

---

## 8. Production Health & Operations Endpoints (`/actuator`)

### 8.1 Public Health Check Probe
- **Endpoint**: `GET /actuator/health`
- **Auth**: Public (Anonymous)
- **Description**: Standard health and liveness probe for cloud orchestrators (Kubernetes, AWS ALB, Render, Railway).
- **Response**: `200 OK`
```json
{
  "status": "UP"
}
```

### 8.2 Public Application Info
- **Endpoint**: `GET /actuator/info`
- **Auth**: Public (Anonymous)
- **Response**: `200 OK`

### 8.3 Protected Administrative Actuator Endpoints
- **Endpoint**: `/actuator/**`
- **Auth**: `ROLE_ADMIN`
- **Description**: Sensitive operational endpoints are strictly guarded against unauthorized inspection. Anonymous requests yield `401 Unauthorized`; customers yield `403 Forbidden`.

# UrbanVogue — Complete Testing Guide

## Step 0: Database Setup (MySQL)

Run these SQL statements first:

```sql
CREATE DATABASE urbanvogue_auth;
CREATE DATABASE productdb;
CREATE DATABASE urbanvogue_order;
CREATE DATABASE urbanvogue_inventory;
CREATE DATABASE urbanvogue_payment;
```

> Tables are auto-created by Hibernate when you start the services.

---

## Step 1: Start All Services

Start each service in a **separate terminal**, in this order:

```bash
cd auth-service        && mvn spring-boot:run
cd product-service     && mvn spring-boot:run
cd inventory-service   && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
cd payment-service     && mvn spring-boot:run
cd order-service       && mvn spring-boot:run
cd api-gateway         && mvn spring-boot:run
```

Wait for each to show `Started ... Application` before starting the next.

---

## Step 2: Test Auth Service (Port 8082)

### 2.1 Register a normal user
```
POST http://localhost:8082/api/auth/register

Headers:
  Content-Type: application/json

Body:
{
  "username": "mithun",
  "email": "mithun@gmail.com",
  "password": "password123"
}
```
✅ Expected: `User registered successfully`

### 2.2 Register an admin user
```
POST http://localhost:8082/api/auth/register

Body:
{
  "username": "admin",
  "email": "admin@gmail.com",
  "password": "admin123"
}
```

Then **promote to ADMIN** in MySQL:
```sql
UPDATE urbanvogue_auth.users SET role = 'ADMIN' WHERE email = 'admin@gmail.com';
```

### 2.3 Login as admin (copy the token!)
```
POST http://localhost:8082/api/auth/login

Body:
{
  "email": "admin@gmail.com",
  "password": "admin123"
}
```
✅ Expected: `{ "token": "eyJhbGciOiJIUzI1NiJ9..." }`

⚠️ **COPY THIS TOKEN** — you'll need it for product creation.

### 2.4 Login as normal user
```
POST http://localhost:8082/api/auth/login

Body:
{
  "email": "mithun@gmail.com",
  "password": "password123"
}
```
✅ Expected: `{ "token": "eyJhbGciOiJIUzI1NiJ9..." }`

---

## Step 3: Test Product Service (Port 8083)

### 3.1 Create products (requires ADMIN token)
```
POST http://localhost:8083/api/products

Headers:
  Content-Type: application/json
  Authorization: Bearer <PASTE_ADMIN_TOKEN_HERE>

Body:
{
  "name": "Black T-Shirt",
  "description": "Premium cotton black t-shirt",
  "price": 24.99
}
```
✅ Expected: Product created with `id: 1`

### 3.2 Create more products
```
POST http://localhost:8083/api/products

Headers:
  Authorization: Bearer <PASTE_ADMIN_TOKEN_HERE>

Body:
{
  "name": "Blue Jeans",
  "description": "Slim fit blue denim jeans",
  "price": 49.99
}
```

```
POST http://localhost:8083/api/products

Headers:
  Authorization: Bearer <PASTE_ADMIN_TOKEN_HERE>

Body:
{
  "name": "White Sneakers",
  "description": "Classic white leather sneakers",
  "price": 79.99
}
```

### 3.3 Get all products (no auth needed)
```
GET http://localhost:8083/api/products
```
✅ Expected: Array of all products

### 3.4 Get single product
```
GET http://localhost:8083/api/products/1
```
✅ Expected: Black T-Shirt details

---

## Step 4: Add Inventory (MySQL)

```sql
INSERT INTO urbanvogue_inventory.inventory (product_id, available_quantity, reserved_quantity) VALUES (1, 100, 0);
INSERT INTO urbanvogue_inventory.inventory (product_id, available_quantity, reserved_quantity) VALUES (2, 50, 0);
INSERT INTO urbanvogue_inventory.inventory (product_id, available_quantity, reserved_quantity) VALUES (3, 75, 0);
```

---

## Step 5: Test Inventory Service (Port 8086)

### 5.1 Check inventory
```
GET http://localhost:8086/api/inventory/1
```
✅ Expected: `{ "id": 1, "productId": 1, "availableQuantity": 100, "reservedQuantity": 0 }`

### 5.2 Deduct inventory
```
POST http://localhost:8086/api/inventory/deduct?productId=1&quantity=5
```
✅ Expected: 200 OK (quantity reduced to 95)

### 5.3 Restore inventory
```
POST http://localhost:8086/api/inventory/restore?productId=1&quantity=5
```
✅ Expected: 200 OK (quantity restored to 100)

---

## Step 6: Test Notification Service (Port 8088)

```
POST http://localhost:8088/api/notifications/send-email

Headers:
  Content-Type: application/json

Body:
{
  "to": "your-email@gmail.com",
  "subject": "Test from UrbanVogue",
  "body": "Hello! If you see this, the email service is working!"
}
```
✅ Expected: `Email sent successfully!` + check your inbox

---

## Step 7: Test Payment Service (Port 8087)

```
POST http://localhost:8087/payments

Headers:
  Content-Type: application/json

Body:
{
  "orderId": 1,
  "amount": 24.99,
  "paymentMethod": "CARD"
}
```
✅ Expected:
```json
{
  "transactionId": "some-uuid",
  "paymentStatus": "PENDING",
  "checkoutUrl": "https://checkout.stripe.com/pay/cs_test_..."
}
```

Open `checkoutUrl` in browser → use test card `4242 4242 4242 4242` → any future expiry → any CVC.

---

## Step 8: Test Full Order Flow (Port 8085)

### 8.1 Create an order
```
POST http://localhost:8085/orders

Headers:
  Content-Type: application/json

Body:
{
  "userId": 1,
  "userEmail": "urbanvogue056@gmail.com",
  "items": [
    {
      "productId": 1,
      "quantity": 2
    },
    {
      "productId": 2,
      "quantity": 1
    }
  ]
}
```
✅ Expected:
```json
{
  "orderId": 1,
  "totalAmount": 99.97,
  "status": "PAID"
}
```

### 8.2 Get order by ID
```
GET http://localhost:8085/orders/1
```
✅ Expected: Full order with items, prices, and status

### 8.3 Get orders by user
```
GET http://localhost:8085/orders/user/1
```
✅ Expected: List of all orders for user 1

### 8.4 Update order status (manual webhook substitute)
```
PUT http://localhost:8085/orders/1/status?status=PAID
```
✅ Expected: `Order status updated to PAID`

---

## Step 9: Test via API Gateway (Port 8080)

All the same endpoints, but through the gateway (single port):

```
POST http://localhost:8080/api/auth/register
POST http://localhost:8080/api/auth/login
GET  http://localhost:8080/api/products
POST http://localhost:8080/api/products          (with ADMIN token)
GET  http://localhost:8080/api/inventory/1
POST http://localhost:8080/orders
GET  http://localhost:8080/orders/1
POST http://localhost:8080/payments
POST http://localhost:8080/api/notifications/send-email
```

All work the same way — the gateway just forwards to the correct service.

---

## New Laptop Setup Checklist

1. Install **Java 17**, **Maven**, **MySQL**
2. Create the 5 databases (Step 0)
3. Update MySQL credentials in each service's `application.properties` if different from `root` / `root@39`
4. Update `notification-service/application.properties` with your Gmail + App Password
5. Update `payment-service/application.properties` with your Stripe API key
6. Start all 7 services (Step 1)
7. Run SQL inserts for test data (Steps 2.2 and 4)
8. Test using the endpoints above

---

## Stripe Test Card Details

| Field | Value |
|---|---|
| Card Number | `4242 4242 4242 4242` |
| Expiry | Any future date (e.g. `12/30`) |
| CVC | Any 3 digits (e.g. `123`) |
| Name | Anything |
| ZIP | Any 5 digits |

---

## Step 10: After Stripe Payment (Manual Webhook Replacement)

Since webhook is not configured yet, do these steps manually after completing Stripe checkout:

### 10.1 Update order status to PAID
```
PUT http://localhost:8085/orders/{orderId}/status?status=PAID
```
Replace `{orderId}` with the actual order ID (e.g., `1`).

✅ Expected: `Order status updated to PAID`

### 10.2 Send email notification
```
POST http://localhost:8088/api/notifications/send-email

Headers:
  Content-Type: application/json

Body:
{
  "to": "urbanvogue056@gmail.com",
  "subject": "UrbanVogue Receipt: Order #1",
  "body": "Thank you for your purchase! Your payment of $99.97 was successful."
}
```
✅ Expected: `Email sent successfully!` + check your inbox

### 10.3 Verify payment in Stripe Dashboard
Go to: https://dashboard.stripe.com/test/payments
✅ You should see your test payment listed there.

---

## TODO: After Adding Webhook

> ⚠️ When Stripe CLI is set up later, remember to also change the order status flow:
> - Order should be `CREATED` initially (not `PAID`)
> - After Stripe payment → webhook fires → status changes to `PAID` automatically
> - Change `OrderService.java` line 92: `savedOrder.setStatus("PAID")` → `savedOrder.setStatus("PENDING")`
> - The webhook in PaymentService will handle updating to `PAID` + sending the email

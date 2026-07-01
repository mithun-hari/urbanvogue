# 📖 UrbanVogue — Technical Documentation

Comprehensive technical documentation for the UrbanVogue e-commerce microservices platform.

---

## Table of Contents

- [System Overview](#system-overview)
- [Microservices](#microservices)
  - [API Gateway](#1-api-gateway)
  - [Auth Service](#2-auth-service)
  - [Product Service](#3-product-service)
  - [Order Service](#4-order-service)
  - [Inventory Service](#5-inventory-service)
  - [Payment Service](#6-payment-service)
  - [Notification Service](#7-notification-service)
- [Frontend Application](#frontend-application)
- [Database Schema](#database-schema)
- [Message Queue Events](#message-queue-events)
- [Authentication Flow](#authentication-flow)
- [Payment Flow](#payment-flow)
- [Order Lifecycle](#order-lifecycle)

---

## System Overview

UrbanVogue is a microservices-based e-commerce platform built with Spring Boot 3.5 and React 18. The system follows a service-per-database pattern where each microservice owns its data store. Inter-service communication is handled through:

- **Synchronous REST calls** via the API Gateway for real-time operations
- **Asynchronous messaging** via RabbitMQ (CloudAMQP) for event-driven workflows (payment events, email notifications)
- **Distributed tracing** via Zipkin and Micrometer Brave for observability across all services

---

## Microservices

### 1. API Gateway

| Property | Value |
|---|---|
| **Port** | `8080` |
| **Framework** | Spring Cloud Gateway (WebMVC) |
| **Purpose** | Centralized request routing and JWT authentication |

**Key Components:**
- **Route Configuration** — Maps incoming paths to backend services:
  - `/api/auth/**` → Auth Service (`:8082`)
  - `/api/products/**` → Product Service (`:8083`)
  - `/orders/**` → Order Service (`:8085`)
  - `/api/inventory/**` → Inventory Service (`:8086`)
  - `/payments/**` → Payment Service (`:8087`)
- **JWT Authentication Filter** — Validates Bearer tokens on protected routes, extracts user claims, and forwards them as headers to downstream services
- **CORS Configuration** — Allows requests from the frontend at `localhost:5173`

**Open Endpoints (no JWT required):**
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/products` and `GET /api/products/{id}`
- `POST /payments/webhook`

---

### 2. Auth Service

| Property | Value |
|---|---|
| **Port** | `8082` |
| **Database** | `urbanvogue_auth` |
| **Purpose** | User registration, login, and JWT token issuance |

**Key Components:**
- `AuthController` — REST endpoints for `/api/auth/register` and `/api/auth/login`
- `AuthService` — Business logic for user registration (with BCrypt password hashing) and authentication
- `JwtService` — JWT token generation and validation using JJWT library
- `JwtAuthenticationFilter` — Spring Security filter for request authentication
- `SecurityConfig` — Spring Security configuration with stateless session management

**User Model:**
| Field | Type | Notes |
|---|---|---|
| `id` | Long | Auto-generated primary key |
| `name` | String | User's display name |
| `email` | String | Unique, used for login |
| `password` | String | BCrypt hashed |
| `role` | String | `USER` or `ADMIN` |

**JWT Token Claims:**
- Subject: user email
- Custom claims: `userId`, `name`, `role`
- Expiration: 1 hour (3,600,000 ms)

---

### 3. Product Service

| Property | Value |
|---|---|
| **Port** | `8083` |
| **Database** | `productdb` |
| **Purpose** | Product catalog management (CRUD) |

**Key Components:**
- `ProductController` — REST endpoints for product CRUD operations
- `ProductService` — Business logic for product management
- `JwtAuthenticationFilter` / `JwtService` — Local JWT validation for protected endpoints
- `SecurityConfig` — GET requests are public; POST/PUT/DELETE require authentication

**Product Model:**
| Field | Type | Notes |
|---|---|---|
| `id` | Long | Auto-generated primary key |
| `name` | String | Product name |
| `description` | String | Product description |
| `price` | Double | Price in USD |
| `imageUrl` | String | Product image URL |
| `category` | String | Product category |

---

### 4. Order Service

| Property | Value |
|---|---|
| **Port** | `8085` |
| **Database** | `urbanvogue_order` |
| **Purpose** | Order creation, tracking, and lifecycle management |

**Key Components:**
- `OrderController` — REST endpoints for order management
- `OrderService` — Business logic for order creation (validates products, checks inventory, reserves stock)
- `PaymentCompletedListener` — RabbitMQ listener that updates order status to `PAID` when payment succeeds
- `PaymentFailedListener` — RabbitMQ listener that updates order status to `FAILED` when payment fails
- `MessagingConfig` — Declares RabbitMQ queues and exchanges

**Inter-Service Communication:**
- Calls **Product Service** to validate products and fetch prices
- Calls **Inventory Service** to check and reserve stock
- Calls **Payment Service** to initiate payment processing
- Listens for `payment.completed` and `payment.failed` events from RabbitMQ

**Order Model:**
| Field | Type | Notes |
|---|---|---|
| `id` | Long | Auto-generated primary key |
| `userId` | Long | Reference to the authenticated user |
| `status` | String | `PENDING`, `PAID`, or `FAILED` |
| `totalAmount` | Double | Calculated total |
| `createdAt` | LocalDateTime | Order creation timestamp |

**OrderItem Model:**
| Field | Type | Notes |
|---|---|---|
| `id` | Long | Auto-generated primary key |
| `orderId` | Long | Foreign key to Order |
| `productId` | Long | Reference to product |
| `productName` | String | Snapshot of product name at time of order |
| `quantity` | Integer | Quantity ordered |
| `price` | Double | Snapshot of price at time of order |

---

### 5. Inventory Service

| Property | Value |
|---|---|
| **Port** | `8086` |
| **Database** | `urbanvogue_inventory` |
| **Purpose** | Stock level tracking and reservation |

**Key Components:**
- `InventoryController` — REST endpoints for stock management
- `InventoryService` — Business logic for stock checks and updates

**Inventory Model:**
| Field | Type | Notes |
|---|---|---|
| `id` | Long | Auto-generated primary key |
| `productId` | Long | Reference to product |
| `quantity` | Integer | Available stock |

---

### 6. Payment Service

| Property | Value |
|---|---|
| **Port** | `8087` |
| **Database** | `urbanvogue_payment` |
| **Purpose** | Stripe payment processing and event publishing |

**Key Components:**
- `PaymentController` — REST endpoint for creating Stripe Checkout sessions and handling webhooks
- `PaymentService` — Business logic for payment creation and Stripe Checkout session management
- `MessagingConfig` — Declares RabbitMQ exchanges and queues for payment events

**Payment Flow:**
1. Frontend calls `POST /payments/checkout` with order ID
2. Service creates a Stripe Checkout Session with line items
3. Returns the Stripe session URL to the frontend
4. Frontend redirects user to Stripe-hosted checkout page
5. On payment completion, Stripe calls `POST /payments/webhook`
6. Webhook handler verifies the Stripe signature and publishes:
   - `payment.completed` event (on success) → Order Service updates status to `PAID`
   - `payment.failed` event (on failure) → Order Service updates status to `FAILED`
7. On `PAID`, the Notification Service sends a confirmation email

**Payment Model:**
| Field | Type | Notes |
|---|---|---|
| `id` | Long | Auto-generated primary key |
| `orderId` | Long | Reference to order |
| `amount` | Double | Payment amount |
| `status` | String | `PENDING`, `COMPLETED`, `FAILED` |
| `stripeSessionId` | String | Stripe Checkout session ID |
| `createdAt` | LocalDateTime | Payment creation timestamp |

---

### 7. Notification Service

| Property | Value |
|---|---|
| **Port** | `8088` |
| **Database** | None (stateless) |
| **Purpose** | Email notifications triggered by RabbitMQ events |

**Key Components:**
- `OrderPaidListener` — RabbitMQ listener for `order.paid` events
- `EmailService` — Sends HTML emails via Gmail SMTP (JavaMailSender)
- `NotificationController` — REST endpoint for manual email triggers
- `MessagingConfig` — Declares the `order.paid.queue` and bindings

**Email Configuration:**
- SMTP: Gmail (`smtp.gmail.com:587`)
- Uses application-specific password (App Password)
- Sends order confirmation with order details

---

## Frontend Application

### Tech Stack
- **React 18** with JSX
- **Vite 6** for build tooling & development server
- **React Router 6** for client-side routing
- **Axios** for HTTP requests
- **Context API** for global state management (Auth + Cart)

### Pages

| Page | Route | Auth Required | Description |
|---|---|---|---|
| Home | `/` | ❌ | Landing page with hero section |
| Login | `/login` | ❌ | User login form |
| Register | `/register` | ❌ | User registration form |
| Products | `/products` | ❌ | Product listing grid |
| Product Detail | `/products/:id` | ❌ | Individual product view |
| Cart | `/cart` | ❌ | Shopping cart |
| Checkout | `/checkout` | ✅ | Order summary & payment |
| Orders | `/orders` | ✅ | Order history list |
| Order Detail | `/orders/:id` | ✅ | Individual order details |
| Dashboard | `/dashboard` | ✅ | Admin dashboard |
| Payment Success | `/payment/success` | ❌ | Post-payment confirmation |

### State Management

**AuthContext** — Manages user authentication state:
- Stores JWT token and user info in `localStorage`
- Provides `login()`, `logout()`, and `isAuthenticated` to child components
- Axios interceptor automatically attaches `Authorization: Bearer <token>` header

**CartContext** — Manages shopping cart state:
- Stores cart items in `localStorage` for persistence
- Provides `addToCart()`, `removeFromCart()`, `updateQuantity()`, `clearCart()`
- Calculates totals automatically

### API Proxy Configuration

The Vite dev server proxies API requests to the backend gateway:

| Frontend Path | Proxied To |
|---|---|
| `/api/auth/*` | `http://localhost:8080` |
| `/api/products/*` | `http://localhost:8080` |
| `/orders/*` | `http://localhost:8080` |
| `/api/inventory/*` | `http://localhost:8080` |
| `/payments/*` | `http://localhost:8080` |

---

## Database Schema

The platform uses **5 separate MySQL databases**, one per data-owning service:

```
┌─────────────────┐  ┌───────────────┐  ┌──────────────────┐
│ urbanvogue_auth │  │   productdb   │  │ urbanvogue_order │
│                 │  │               │  │                  │
│ • users         │  │ • products    │  │ • orders         │
│                 │  │               │  │ • order_items    │
└─────────────────┘  └───────────────┘  └──────────────────┘

┌─────────────────────┐  ┌─────────────────────┐
│ urbanvogue_inventory│  │ urbanvogue_payment   │
│                     │  │                      │
│ • inventory         │  │ • payments           │
└─────────────────────┘  └──────────────────────┘
```

All tables are auto-created by JPA with `ddl-auto=update`.

---

## Message Queue Events

UrbanVogue uses RabbitMQ (hosted on CloudAMQP) for asynchronous event-driven communication:

| Event | Publisher | Consumer | Trigger |
|---|---|---|---|
| `payment.completed` | Payment Service | Order Service | Stripe webhook confirms successful payment |
| `payment.failed` | Payment Service | Order Service | Stripe webhook reports failed payment |
| `order.paid` | Order Service | Notification Service | Order status updated to `PAID` |

### Event Flow Diagram

```
Payment Service                Order Service               Notification Service
      │                              │                              │
      │ ── payment.completed ──────► │                              │
      │                              │ ── order.paid ─────────────► │
      │                              │                              │ → Send Email
      │ ── payment.failed ────────► │                              │
      │                              │ (status → FAILED)           │
```

---

## Authentication Flow

```
┌──────────┐          ┌──────────┐          ┌──────────┐
│ Frontend │          │ Gateway  │          │  Auth    │
│          │          │  :8080   │          │  :8082   │
└────┬─────┘          └────┬─────┘          └────┬─────┘
     │                     │                     │
     │ POST /api/auth/login│                     │
     │ ──────────────────► │                     │
     │                     │ ──────────────────► │
     │                     │                     │ Validate credentials
     │                     │                     │ Generate JWT
     │                     │ ◄──────────────────│ Return { token, user }
     │ ◄──────────────────│                     │
     │                     │                     │
     │ Store token in      │                     │
     │ localStorage        │                     │
     │                     │                     │
     │ GET /api/products   │                     │
     │ Authorization:      │                     │
     │ Bearer <token>      │                     │
     │ ──────────────────► │                     │
     │                     │ Validate JWT        │
     │                     │ Extract claims       │
     │                     │ Forward to service   │
     │                     │                     │
```

---

## Payment Flow

```
┌──────────┐    ┌────────┐    ┌─────────┐    ┌────────┐    ┌────────────┐
│ Frontend │    │Gateway │    │ Payment │    │ Stripe │    │Notification│
└────┬─────┘    └───┬────┘    └────┬────┘    └───┬────┘    └─────┬──────┘
     │              │              │              │               │
     │ POST /payments/checkout     │              │               │
     │ ───────────► │ ───────────► │              │               │
     │              │              │ Create       │               │
     │              │              │ Session ────►│               │
     │              │              │ ◄────────────│               │
     │ ◄─────────── │ ◄─────────── │              │               │
     │ { checkoutUrl }             │              │               │
     │                             │              │               │
     │ Redirect to Stripe ────────────────────── ►│               │
     │                             │              │               │
     │                             │   Webhook    │               │
     │                             │ ◄────────────│               │
     │                             │              │               │
     │                             │ Publish      │               │
     │                             │ payment.completed             │
     │                             │ ──────────── ► Order Service  │
     │                             │               │ ────────────► │
     │                             │               │  Send Email   │
     │ ◄─── Redirect to /payment/success           │               │
```

---

## Order Lifecycle

```
                    ┌─────────┐
                    │ PENDING │
                    └────┬────┘
                         │
                    Payment initiated
                         │
              ┌──────────┴──────────┐
              │                     │
    Stripe success           Stripe failure
              │                     │
              ▼                     ▼
        ┌──────────┐         ┌──────────┐
        │   PAID   │         │  FAILED  │
        └──────────┘         └──────────┘
              │
        Email sent to
         customer
```

---

> For API setup and quick start instructions, see the main [README.md](README.md).

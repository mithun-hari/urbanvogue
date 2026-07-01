<div align="center">

# 🛍️ UrbanVogue

### A Modern E-Commerce Platform Built with Microservices Architecture

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)](https://www.rabbitmq.com)
[![Stripe](https://img.shields.io/badge/Stripe-008CDD?style=for-the-badge&logo=stripe&logoColor=white)](https://stripe.com)

---

*A full-stack, production-ready e-commerce platform featuring JWT authentication, Stripe payments, real-time email notifications, distributed tracing, and a React storefront — all orchestrated through an API Gateway.*

[Features](#-features) · [Architecture](#-architecture) · [Getting Started](#-getting-started) · [API Reference](#-api-reference) · [Documentation](DOCS.md)

</div>

---

## ✨ Features

| Category | Details |
|---|---|
| **🔐 Authentication** | JWT-based registration & login with Spring Security |
| **📦 Product Management** | Full CRUD operations with role-based access control |
| **🛒 Shopping Cart** | Client-side cart with persistent state across sessions |
| **💳 Payments** | Stripe Checkout integration with webhook verification |
| **📋 Order Management** | Complete order lifecycle tracking (PENDING → PAID → FAILED) |
| **📊 Inventory** | Real-time stock tracking with reservation support |
| **📧 Notifications** | Automated order confirmation emails via Gmail SMTP |
| **🔗 API Gateway** | Centralized routing with JWT validation filter |
| **📡 Distributed Tracing** | End-to-end request tracing with Zipkin + Micrometer |
| **💬 Async Messaging** | Event-driven communication via RabbitMQ (CloudAMQP) |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     React Frontend (:5173)                  │
│              Vite + React Router + Axios                    │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│                    API Gateway (:8080)                       │
│           Spring Cloud Gateway + JWT Filter                  │
└──┬──────────┬──────────┬──────────┬──────────┬───────────────┘
   │          │          │          │          │
   ▼          ▼          ▼          ▼          ▼
┌──────┐ ┌───────┐ ┌────────┐ ┌─────────┐ ┌─────────┐
│ Auth │ │Product│ │  Order │ │Inventory│ │ Payment │
│:8082 │ │ :8083 │ │ :8085  │ │  :8086  │ │  :8087  │
└──────┘ └───────┘ └───┬────┘ └─────────┘ └────┬────┘
                       │                        │
                       │   ┌────────────────┐   │
                       └──►│   RabbitMQ      │◄──┘
                           │  (CloudAMQP)   │
                           └───────┬────────┘
                                   │
                                   ▼
                          ┌────────────────┐
                          │  Notification  │
                          │    :8088       │
                          └────────────────┘
```

**Data Flow:**
1. Frontend sends requests through the **API Gateway**
2. Gateway validates JWT tokens and routes to the appropriate microservice
3. **Order Service** communicates with Product, Inventory, and Payment services via REST
4. **Payment Service** creates Stripe Checkout sessions and publishes events to RabbitMQ on success/failure
5. **Order Service** listens for payment events and updates order status
6. **Notification Service** listens for `order.paid` events and sends confirmation emails

---

## 🛠️ Tech Stack

### Backend
| Technology | Purpose |
|---|---|
| **Java 17** | Runtime |
| **Spring Boot 3.5** | Framework for all microservices |
| **Spring Security** | Authentication & authorization |
| **Spring Data JPA** | Database ORM |
| **Spring Cloud Gateway** | API Gateway & routing |
| **MySQL** | Relational database (separate DB per service) |
| **RabbitMQ (CloudAMQP)** | Asynchronous event-driven messaging |
| **Stripe API** | Payment processing |
| **Zipkin + Micrometer** | Distributed tracing |
| **Lombok** | Boilerplate reduction |
| **JJWT 0.11.5** | JWT token generation & validation |

### Frontend
| Technology | Purpose |
|---|---|
| **React 18** | UI library |
| **Vite 6** | Build tool & dev server |
| **React Router 6** | Client-side routing |
| **Axios** | HTTP client |
| **Vanilla CSS** | Styling |

---

## 🚀 Getting Started

### Prerequisites

- **Java 17+** — [Download](https://adoptium.net/)
- **Maven 3.9+** — [Download](https://maven.apache.org/download.cgi)
- **Node.js 18+** — [Download](https://nodejs.org/)
- **MySQL 8.0** — [Download](https://dev.mysql.com/downloads/)

### 1. Database Setup

Create the required MySQL databases:

```sql
CREATE DATABASE urbanvogue_auth;
CREATE DATABASE productdb;
CREATE DATABASE urbanvogue_order;
CREATE DATABASE urbanvogue_inventory;
CREATE DATABASE urbanvogue_payment;
```

### 2. Start Backend Services

Start each service from the project root. They must be started in this order:

```bash
# 1. API Gateway (port 8080)
cd api-gateway
./mvnw spring-boot:run

# 2. Auth Service (port 8082)
cd auth-service
./mvnw spring-boot:run

# 3. Product Service (port 8083)
cd product-service
./mvnw spring-boot:run

# 4. Order Service (port 8085)
cd order-service
./mvnw spring-boot:run

# 5. Inventory Service (port 8086)
cd inventory-service
./mvnw spring-boot:run

# 6. Payment Service (port 8087)
cd payment-service
./mvnw spring-boot:run

# 7. Notification Service (port 8088)
cd notification-service
./mvnw spring-boot:run
```

### 3. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

The app will be available at **http://localhost:5173**

### 4. (Optional) Start Zipkin

For distributed tracing, run a Zipkin server:

```bash
# Using Docker
docker run -d -p 9411:9411 openzipkin/zipkin

# Zipkin UI available at http://localhost:9411
```

---

## 📡 API Reference

All API requests are routed through the **API Gateway** at `http://localhost:8080`.

### Authentication
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `POST` | `/api/auth/register` | Register a new user | ❌ |
| `POST` | `/api/auth/login` | Login & receive JWT token | ❌ |

### Products
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `GET` | `/api/products` | List all products | ❌ |
| `GET` | `/api/products/{id}` | Get product by ID | ❌ |
| `POST` | `/api/products` | Create a product | ✅ |
| `PUT` | `/api/products/{id}` | Update a product | ✅ |
| `DELETE` | `/api/products/{id}` | Delete a product | ✅ |

### Orders
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `POST` | `/orders` | Create a new order | ✅ |
| `GET` | `/orders` | Get user's orders | ✅ |
| `GET` | `/orders/{id}` | Get order details | ✅ |

### Payments
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `POST` | `/payments/checkout` | Create Stripe checkout session | ✅ |
| `POST` | `/payments/webhook` | Stripe webhook handler | ❌ |

### Inventory
| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `GET` | `/api/inventory/{productId}` | Check stock level | ✅ |
| `PUT` | `/api/inventory/{productId}` | Update stock | ✅ |

---

## 📁 Project Structure

```
urbanvogue/
├── api-gateway/           # Spring Cloud Gateway — routing & JWT filter
├── auth-service/          # User registration, login & JWT issuance
├── product-service/       # Product CRUD operations
├── order-service/         # Order creation & lifecycle management
├── inventory-service/     # Stock level tracking
├── payment-service/       # Stripe integration & payment processing
├── notification-service/  # Email notifications via RabbitMQ events
├── frontend/              # React SPA with Vite
│   ├── src/
│   │   ├── api/           # Axios API clients
│   │   ├── components/    # Reusable UI components
│   │   ├── context/       # Auth & Cart context providers
│   │   ├── hooks/         # Custom React hooks
│   │   └── pages/         # Route page components
│   └── vite.config.js     # Dev server & proxy configuration
└── README.md
```

---

## 🔐 Environment Configuration

Each microservice has its own `application.properties` inside `src/main/resources/`. Key configuration values you may need to update:

| Service | Property | Description |
|---|---|---|
| **All services** | `spring.datasource.*` | MySQL connection details |
| **Auth** | `jwt.secret` | JWT signing key |
| **Payment** | `stripe.api.key` | Stripe secret key |
| **Payment** | `stripe.webhook.secret` | Stripe webhook signing secret |
| **Notification** | `spring.mail.*` | Gmail SMTP credentials |
| **Order/Payment/Notification** | `spring.rabbitmq.addresses` | CloudAMQP connection URL |

> ⚠️ **Security Note:** For production, move sensitive values (API keys, passwords, secrets) to environment variables or a secrets manager.

---

## 🧪 Testing

Each backend service includes unit and integration tests:

```bash
# Run tests for a specific service
cd product-service
./mvnw test

# Run integration tests
./mvnw test -Dtest="*IntegrationTest"

# Run performance tests
./mvnw test -Dtest="*PerformanceTest"
```

---

## 👤 Author

**Mithun Hari**

- GitHub: [@mithun-hari](https://github.com/mithun-hari)

---

<div align="center">
  <sub>Built with ❤️ using Spring Boot & React</sub>
</div>

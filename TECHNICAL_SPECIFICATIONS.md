# urbanVogue E-Commerce Platform
## Technical Specifications & Implementation Details

**Version:** 1.0  
**Date:** April 8, 2026  
**Prepared By:** Development Team  

---

## Table of Contents

1. [System Requirements](#system-requirements)
2. [Architecture Specifications](#architecture-specifications)
3. [Service Specifications](#service-specifications)
4. [Data Models](#data-models)
5. [API Specifications](#api-specifications)
6. [Integration Specifications](#integration-specifications)
7. [Security Specifications](#security-specifications)
8. [Performance Requirements](#performance-requirements)

---

## System Requirements

### Hardware Requirements

**Development Environment:**
- CPU: Dual-core (4+ cores recommended)
- RAM: 8GB minimum (16GB recommended)
- Storage: 50GB SSD for development
- Network: Stable internet connection

**Production Environment:**
- CPU: 8+ cores per instance
- RAM: 16GB+ per instance
- Storage: High-speed SSD with auto-scaling
- Network: Redundant connections with CDN

### Software Requirements

**Backend:**
- Java Development Kit (JDK) 17 or higher
- Maven 3.8.x or higher
- MySQL 5.7 or MySQL 8.0
- RabbitMQ 3.x
- Redis 6.x (for caching layer)

**Frontend:**
- Node.js 18.x or higher
- npm 9.x or higher
- Git 2.x

**Infrastructure:**
- Docker 20.x (for containerization)
- Docker Compose 2.x
- Kubernetes 1.25+ (for production)
- nginx or Apache (for reverse proxy)

---

## Architecture Specifications

### System Topology

```
┌─────────────────────────────────────────────────────────────┐
│                    Internet Users                            │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
        ┌────────────────────────────┐
        │   Load Balancer (nginx)    │
        │   Port: 80/443             │
        └────────────┬───────────────┘
                     │
          ┌──────────┴──────────┐
          │                     │
          ▼                     ▼
    ┌──────────────┐    ┌──────────────┐
    │  Frontend    │    │ API Gateway  │
    │  Instance 1  │    │   Instance   │
    │  Port: 5173  │    │   Port: 8080 │
    └──────────────┘    └──────┬───────┘
                                │
                ┌───────────────┼───────────────┐
                │               │               │
                ▼               ▼               ▼
         ┌────────────┐  ┌────────────┐  ┌────────────┐
         │Auth Svc    │  │Product Svc │  │Order Svc   │
         │Port: 8082  │  │Port: 8083  │  │Port: 8085  │
         └────────────┘  └────────────┘  └────────────┘

         ┌────────────┐  ┌────────────┐  ┌────────────┐
         │Inventory   │  │Payment Svc │  │Notif Svc   │
         │Port: 8086  │  │Port: 8087  │  │Port: 8088  │
         └────────────┘  └────────────┘  └────────────┘

Database Layer (MySQL):
┌─────────────────────────────────────────────────────────┐
│ urbanvogue_auth │ productdb │ urbanvogue_inventory      │
│ urbanvogue_order│ urbanvogue_payment                    │
└─────────────────────────────────────────────────────────┘

Message Broker:
┌──────────────────────────────────────────────────────────┐
│        RabbitMQ (Event Distribution)                     │
│ Exchanges: order.exchange, payment.exchange              │
│ Queues: notification.queue, order.queue, payment.queue   │
└──────��───────────────────────────────────────────────────┘

External Services:
┌──────────────────────────────────────────────────────────┐
│ Stripe API (Payment Processing)  Gmail SMTP (Emails)    │
└──────────────────────────────────────────────────────────┘
```

### Service Dependencies

```
Frontend (React)
    │
    ├─→ API Gateway (Authentication & Routing)
            │
            ├─→ Auth Service (JWT Validation)
            ├─→ Product Service (Catalog)
            ├─→ Order Service (Order Management)
            ├─→ Inventory Service (Stock)
            ├─→ Payment Service (Payments)
            └─→ Notification Service (Emails)

Order Service
    ├─→ Product Service (Feign: Get Details)
    ├─→ Inventory Service (Feign: Check Stock)
    ├─→ Payment Service (WebClient: Process Payment)
    └─→ RabbitMQ (Publish Events)

Payment Service
    ├─→ Stripe API (HTTPS: Process Payment)
    └─→ RabbitMQ (Publish: Payment Events)

Notification Service
    ├─→ RabbitMQ (Consume: Order Events)
    └─→ Gmail SMTP (Send: Emails)
```

### Deployment Architecture

**Three-Tier Architecture:**

```
Tier 1: Presentation Layer
├─ React Frontend (SPA)
├─ Static Assets (CSS, JS, Images)
└─ Client-side Validation & State Management

Tier 2: Application Layer
├─ API Gateway (Routing & Auth)
├─ Microservices (6 independent services)
├─ Service Discovery & Configuration
└─ Load Balancing & Circuit Breaking

Tier 3: Data Layer
├─ Relational Databases (MySQL)
├─ Message Broker (RabbitMQ)
├─ Cache Layer (Redis - optional)
└─ File Storage (S3 - optional)

External Layer:
├─ Payment Gateway (Stripe)
└─ Email Service (Gmail SMTP)
```

---

## Service Specifications

### API Gateway Specification

**Component:** Spring Cloud Gateway  
**Port:** 8080  
**Functions:**
- Request routing to appropriate microservices
- JWT token validation
- CORS handling
- Rate limiting (ready for implementation)
- Load balancing

**Routes:**
```yaml
/api/auth/**       → Auth Service (8082)
/api/products/**   → Product Service (8083)
/api/orders/**     → Order Service (8085)
/api/inventory/**  → Inventory Service (8086)
/api/payments/**   → Payment Service (8087)
/api/notifications/** → Notification Service (8088)
```

**Filters:**
- RequestLoggingFilter - Log all incoming requests
- JwtAuthenticationFilter - Validate JWT tokens
- CorsFilter - Handle cross-origin requests
- HeaderFilter - Add tracing headers

### Auth Service Specification

**Port:** 8082  
**Database:** urbanvogue_auth  
**Key Entities:**

```java
User {
    Long id (PK)
    String username (UNIQUE, NOT NULL)
    String email (UNIQUE, NOT NULL)
    String password (Encrypted, NOT NULL)
    String role (ENUM: USER, ADMIN)
    LocalDateTime createdAt
    LocalDateTime updatedAt
}
```

**Endpoints:**

| Method | Endpoint | Auth | Purpose |
|--------|----------|------|---------|
| POST | /api/auth/register | No | Register new user |
| POST | /api/auth/login | No | Authenticate user |
| GET | /api/auth/me | Yes | Get current user |
| POST | /api/auth/logout | Yes | Logout user |
| POST | /api/auth/refresh | No | Refresh token |

**Token Structure:**
```
Header: {
  "alg": "HS256",
  "typ": "JWT"
}

Payload: {
  "sub": "user@example.com",
  "id": 1,
  "role": "USER",
  "iat": 1680000000,
  "exp": 1680900000
}

Signature: HMAC-SHA256(secret)
```

### Product Service Specification

**Port:** 8083  
**Database:** productdb  
**Key Entities:**

```java
Product {
    Long id (PK)
    String name (NOT NULL)
    String description
    Double price (NOT NULL, > 0)
    String imageUrl
    String category
    LocalDateTime createdAt
    LocalDateTime updatedAt
}
```

**Endpoints:**

| Method | Endpoint | Params | Purpose |
|--------|----------|--------|---------|
| GET | /api/products | page, size, sort | List products |
| GET | /api/products/{id} | - | Get product |
| POST | /api/products | body | Create product (ADMIN) |
| PUT | /api/products/{id} | body | Update product (ADMIN) |
| DELETE | /api/products/{id} | - | Delete product (ADMIN) |
| GET | /api/products/search | name, minPrice, maxPrice | Search |

**Caching Strategy:**
- Product list: Cache 5 minutes
- Single product: Cache 10 minutes
- Cache invalidation on POST/PUT/DELETE

### Inventory Service Specification

**Port:** 8086  
**Database:** urbanvogue_inventory  
**Key Entities:**

```java
Inventory {
    Long id (PK)
    Long productId (FK to Product)
    Integer availableQuantity (≥ 0)
    Integer reservedQuantity (≥ 0)
    LocalDateTime updatedAt
}
```

**Endpoints:**

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | /api/inventory/{productId} | Get stock status |
| POST | /api/inventory/reserve | Reserve items |
| POST | /api/inventory/deduct | Deduct sold items |
| POST | /api/inventory/restore | Restore on cancellation |

**Operations:**
```java
// Reserve (Order Created)
availableQuantity -= quantity
reservedQuantity += quantity

// Deduct (Payment Success)
reservedQuantity -= quantity

// Restore (Order Cancelled)
availableQuantity += quantity
reservedQuantity -= quantity
```

### Order Service Specification

**Port:** 8085  
**Database:** urbanvogue_order  
**Key Entities:**

```java
Order {
    Long id (PK)
    Long userId (FK)
    String userEmail
    Double totalAmount
    String status (ENUM)
    LocalDateTime createdAt
    LocalDateTime updatedAt
}

OrderItem {
    Long id (PK)
    Long orderId (FK)
    Long productId
    Integer quantity
    Double price
}
```

**Status Transitions:**
```
PENDING → PROCESSING → COMPLETED
         ↓ (payment fails)
         → FAILED
         ↓ (user cancels)
         → CANCELLED
```

**Endpoints:**

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | /api/orders | Create order |
| GET | /api/orders/{id} | Get order details |
| GET | /api/orders/user/{userId} | Get user orders |
| PUT | /api/orders/{id}/status | Update status |
| DELETE | /api/orders/{id} | Cancel order |

**Order Creation Logic:**

```
1. Validate all items exist (ProductClient.getProduct)
2. Check stock (InventoryClient.getInventory)
3. Reserve inventory (InventoryClient.reserve)
4. Create Order in DB with status PENDING
5. Call PaymentClient.processPayment (async)
6. Listen for payment events:
   - SUCCESS: Update order status to COMPLETED
   - FAILED: Restore inventory, set status to FAILED
```

### Payment Service Specification

**Port:** 8087  
**Database:** urbanvogue_payment  
**Key Entities:**

```java
Payment {
    Long id (PK)
    Long orderId (FK)
    Double amount
    String paymentMethod (CARD, UPI, etc.)
    String paymentStatus (PENDING, SUCCESS, FAILED)
    String stripeSessionId
    String transactionId
    LocalDateTime createdAt
}
```

**Endpoints:**

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | /api/payments | Initiate payment |
| GET | /api/payments/{orderId} | Get payment status |
| POST | /api/payments/webhook | Stripe webhook |

**Stripe Integration:**
- Session-based checkout
- Webhook signature verification
- Idempotent payment processing
- Automatic retry on failures

**Event Publishing:**
```
PaymentSuccessListener publishes:
→ PaymentCompletedEvent to order.exchange
→ consumed by Order Service & Notification Service

PaymentFailedListener publishes:
→ PaymentFailedEvent to order.exchange
→ consumed by Order Service
```

### Notification Service Specification

**Port:** 8088  
**Database:** None (state-less)  
**Key Functions:**
- Email sending via SMTP
- Email template rendering
- Event listening from RabbitMQ

**Event Listeners:**

```java
@RabbitListener(queues = NOTIFICATION_QUEUE)
public void handleOrderCreated(OrderCreatedEvent event) {
    // Send order confirmation email
    emailService.sendOrderConfirmation(
        event.getUserEmail(),
        event.getOrderDetails()
    );
}

@RabbitListener(queues = PAYMENT_QUEUE)
public void handlePaymentSuccess(PaymentCompletedEvent event) {
    // Send payment receipt
    emailService.sendPaymentReceipt(
        event.getUserEmail(),
        event.getTransactionId()
    );
}
```

**Email Templates:**
1. Order Confirmation
2. Payment Receipt
3. Order Shipped
4. Order Delivered
5. Support Response

---

## Data Models

### Complete Database Schema

#### Table: users (urbanvogue_auth)
```sql
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role ENUM('USER', 'ADMIN') DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_username (username)
);
```

#### Table: product (productdb)
```sql
CREATE TABLE product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DOUBLE NOT NULL CHECK(price > 0),
    image_url VARCHAR(500),
    category VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_price (price),
    FULLTEXT INDEX ft_name_desc (name, description)
);
```

#### Table: inventory (urbanvogue_inventory)
```sql
CREATE TABLE inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT UNIQUE NOT NULL,
    available_quantity INT DEFAULT 0 CHECK(available_quantity >= 0),
    reserved_quantity INT DEFAULT 0 CHECK(reserved_quantity >= 0),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_id (product_id)
);
```

#### Table: orders (urbanvogue_order)
```sql
CREATE TABLE orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    user_email VARCHAR(100) NOT NULL,
    total_amount DOUBLE NOT NULL CHECK(total_amount > 0),
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

#### Table: order_items (urbanvogue_order)
```sql
CREATE TABLE order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL CHECK(quantity > 0),
    price DOUBLE NOT NULL CHECK(price > 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order_id (order_id),
    INDEX idx_product_id (product_id),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);
```

#### Table: payments (urbanvogue_payment)
```sql
CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    amount DOUBLE NOT NULL CHECK(amount > 0),
    payment_method VARCHAR(50),
    payment_status VARCHAR(20) DEFAULT 'PENDING',
    stripe_session_id VARCHAR(500),
    transaction_id VARCHAR(255) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_id (order_id),
    INDEX idx_payment_status (payment_status),
    INDEX idx_stripe_session (stripe_session_id)
);
```

---

## API Specifications

### Request/Response Format

**Standard Request:**
```json
{
    "Content-Type": "application/json",
    "Authorization": "Bearer <JWT_TOKEN>"
}
```

**Standard Response (Success):**
```json
{
    "code": 200,
    "message": "Operation successful",
    "data": { /* entity data */ },
    "timestamp": "2026-04-08T10:30:00Z"
}
```

**Standard Response (Error):**
```json
{
    "code": 400,
    "message": "Invalid request",
    "errors": [
        {
            "field": "email",
            "message": "Email already exists"
        }
    ],
    "timestamp": "2026-04-08T10:30:00Z"
}
```

### HTTP Status Codes

| Code | Meaning | Usage |
|------|---------|-------|
| 200 | OK | Successful request |
| 201 | Created | Resource created successfully |
| 400 | Bad Request | Invalid input parameters |
| 401 | Unauthorized | Missing/invalid JWT token |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource not found |
| 409 | Conflict | Duplicate resource |
| 500 | Server Error | Unexpected error |
| 503 | Service Unavailable | Service down |

---

## Integration Specifications

### Stripe Integration

**Setup:**
```
1. Create Stripe account (https://stripe.com)
2. Get API keys (publishable & secret)
3. Configure webhook endpoint
4. Set webhook events: charge.succeeded, charge.failed
```

**Payment Flow:**
```
Frontend creates Stripe Elements
       ↓
User enters card details
       ↓
Submit to backend Payment Service
       ↓
Create Stripe Checkout Session
       ↓
Get session ID → Send to frontend
       ↓
Frontend redirects to Stripe Checkout
       ↓
User completes payment
       ↓
Stripe sends webhook to /api/payments/webhook
       ↓
Service verifies signature → Updates payment status
       ↓
Publishes PaymentCompletedEvent
```

**Webhook Example:**
```javascript
POST /api/payments/webhook
Content-Type: application/json
Stripe-Signature: t=timestamp,v1=signature

{
    "id": "evt_1234567890",
    "type": "charge.succeeded",
    "data": {
        "object": {
            "id": "ch_1234567890",
            "amount": 50000,
            "currency": "usd",
            "metadata": {
                "orderId": "101"
            }
        }
    }
}
```

### RabbitMQ Integration

**Configuration:**
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

**Exchanges & Queues:**
```
Exchange: order.exchange (Topic)
├─ Queue: notification.queue
│  └─ Binding: order.created, order.updated
└─ Queue: order.dlq (Dead Letter)

Exchange: payment.exchange (Topic)
├─ Queue: order.queue
│  └─ Binding: payment.success, payment.failed
└─ Queue: notification.queue
   └─ Binding: payment.success
```

**Message Format:**
```json
{
    "eventId": "evt_123456",
    "eventType": "ORDER_CREATED",
    "aggregateId": "order_101",
    "timestamp": "2026-04-08T10:30:00Z",
    "data": {
        "orderId": 101,
        "userId": 1,
        "userEmail": "user@example.com",
        "totalAmount": 599.99
    }
}
```

---

## Security Specifications

### Authentication Flow

```
1. User Registration
   POST /api/auth/register
   Request: {username, email, password}
   Response: User created

2. User Login
   POST /api/auth/login
   Request: {email, password}
   Response: {accessToken, refreshToken}

3. API Request with Token
   GET /api/products
   Header: Authorization: Bearer <accessToken>
   Gateway validates token → Routes to service

4. Token Expiration
   If expired: 401 Unauthorized
   Client requests new token using refreshToken
   POST /api/auth/refresh
   Response: New accessToken
```

### JWT Token Specification

**Access Token:**
- Algorithm: HS256
- Expiry: 15 minutes
- Contains: user ID, email, role

**Refresh Token:**
- Algorithm: HS256
- Expiry: 7 days
- Used to obtain new access token

**Token Validation:**
```java
@Bean
public JwtDecoder jwtDecoder() {
    return NimbusJwtDecoder.withSecretKey(SECRET_KEY).build();
}

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) {
        String token = extractToken(request);
        if (isTokenValid(token)) {
            // Extract claims and set in SecurityContext
            Authentication auth = new UsernamePasswordAuthenticationToken(...);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}
```

### Password Security

- Algorithm: BCrypt
- Salt rounds: 12
- Minimum length: 8 characters
- Requirements: At least one uppercase, one number

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}

// Usage
String encodedPassword = passwordEncoder.encode(rawPassword);
boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
```

### CORS Configuration

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:5173", "https://urbanvogue.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

---

## Performance Requirements

### Latency Targets

| Operation | Target | Current | Status |
|-----------|--------|---------|--------|
| Single order creation | < 500ms | ~150ms | ✅ Exceeded |
| Product search | < 200ms | ~100ms | ✅ Exceeded |
| Order lookup | < 100ms | ~50ms | ✅ Exceeded |
| JWT validation | < 5ms | ~2ms | ✅ Exceeded |
| Database query | < 50ms | ~30ms | ✅ Exceeded |

### Throughput Targets

| Operation | Target | Current | Status |
|-----------|--------|---------|--------|
| Orders/second | > 20 | 40 | ✅ Exceeded |
| API requests/sec | > 100 | 200+ | ✅ Exceeded |
| Database ops/sec | > 1000 | 5000+ | ✅ Exceeded |

### Concurrency Targets

| Metric | Target | Current | Status |
|--------|--------|---------|--------|
| Concurrent users | 50 | 50 | ✅ Met |
| Success rate | 99% | 100% | ✅ Exceeded |
| Avg response (P50) | < 200ms | ~100ms | ✅ Exceeded |
| P95 response | < 300ms | ~250ms | ✅ Exceeded |

### Resource Utilization

| Resource | Limit | Target | Current |
|----------|-------|--------|---------|
| CPU | 80% | < 60% | ✅ Within limits |
| Memory | 85% | < 70% | ✅ Within limits |
| Disk I/O | 80% | < 50% | ✅ Within limits |
| Database Connections | 100 | < 80 | ✅ Within limits |

---

## Monitoring & Observability

### Health Checks

**Endpoint:** `GET /actuator/health`

**Response:**
```json
{
    "status": "UP",
    "components": {
        "db": {"status": "UP"},
        "rabbitmq": {"status": "UP"},
        "diskSpace": {"status": "UP"}
    }
}
```

### Metrics

**Available Metrics:**
- `http.requests.total` - Total HTTP requests
- `http.request.duration.seconds` - Request latency
- `db.connection.pool.active` - Active DB connections
- `rabbitmq.queue.message.count` - Message queue depth
- `jvm.memory.used.bytes` - JVM memory usage

**Endpoint:** `GET /actuator/metrics`

### Distributed Tracing

**Tool:** Zipkin  
**URL:** http://localhost:9411

**Trace ID Flow:**
```
Request → API Gateway (adds trace ID) 
       → Service 1 (propagates trace ID)
       → Service 2 (propagates trace ID)
       → Response
       
Zipkin receives all spans and correlates by trace ID
```

---

## Configuration Management

### Environment Variables

```bash
# Server Configuration
SERVER_PORT=8082
JAVA_OPTS="-Xms512m -Xmx1024m"

# Database
DB_HOST=localhost
DB_PORT=3306
DB_NAME=urbanvogue_auth
DB_USER=root
DB_PASSWORD=password

# RabbitMQ
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest

# Stripe
STRIPE_API_KEY=sk_test_xxx
STRIPE_WEBHOOK_SECRET=whsec_xxx

# JWT
JWT_SECRET=your_super_secret_key_min_256_bits_long
JWT_EXPIRY=900000

# Logging
LOG_LEVEL=INFO
```

### Application Properties

```yaml
# Common configuration
spring:
  application:
    name: auth-service
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true
logging:
  level:
    com.urbanvogue: DEBUG
    org.springframework: INFO
```

---

**End of Technical Specifications**

**Version:** 1.0  
**Last Updated:** April 8, 2026  
**Status:** Complete & Ready for Implementation


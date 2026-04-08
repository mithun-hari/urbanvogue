# urbanVogue E-Commerce Platform
## Quick Start & Setup Guide

**Version:** 1.0  
**Date:** April 8, 2026  

---

## Table of Contents

1. [Local Development Setup](#local-development-setup)
2. [Running the Application](#running-the-application)
3. [Testing the Application](#testing-the-application)
4. [Common Issues & Troubleshooting](#common-issues--troubleshooting)
5. [Project Structure](#project-structure)
6. [Key Files Reference](#key-files-reference)

---

## Local Development Setup

### Prerequisites

**Verify Installation:**

```powershell
# Check Java version (should be 17+)
java -version

# Check Maven version (should be 3.8+)
mvn -version

# Check Node.js version (should be 18+)
node -v

# Check npm version (should be 9+)
npm -v

# Check Git version
git -version

# Check MySQL version
mysql --version

# Check if MySQL service is running
Get-Service MySQL80

# Check if RabbitMQ is running
Get-Service RabbitMQ
```

### Install Dependencies (If Not Installed)

**Java 17:**
```powershell
# Using Chocolatey (if installed)
choco install openjdk17

# Or download from
# https://adoptium.net/temurin/releases/?version=17
```

**Maven:**
```powershell
# Using Chocolatey
choco install maven

# Verify Maven setup
mvn -version
```

**Node.js 18:**
```powershell
# Using Chocolatey
choco install nodejs

# Verify installation
node -v
npm -v
```

**MySQL 5.7/8.0:**
```powershell
# Using Chocolatey
choco install mysql

# Start MySQL service
Start-Service MySQL80

# Connect to MySQL
mysql -u root -p
```

**RabbitMQ:**
```powershell
# Using Chocolatey
choco install rabbitmq

# Start RabbitMQ
Start-Service RabbitMQ

# Enable Management Plugin
rabbitmq-plugins enable rabbitmq_management

# Access management console at http://localhost:15672
# Default credentials: guest / guest
```

---

## Running the Application

### Step 1: Clone the Repository

```powershell
cd C:\Users\mithunhari.k\Desktop
# Repository already exists at urbanvogue
cd urbanvogue
```

### Step 2: Create Databases

**Connect to MySQL:**
```powershell
mysql -u root -p
# Enter your MySQL password
```

**Execute Database Creation Script:**
```sql
-- Create databases
CREATE DATABASE urbanvogue_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE productdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE urbanvogue_inventory CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE urbanvogue_order CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE urbanvogue_payment CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Create application user (optional but recommended)
CREATE USER 'urbanvogue'@'localhost' IDENTIFIED BY 'urbanvogue_password';
GRANT ALL PRIVILEGES ON urbanvogue_auth.* TO 'urbanvogue'@'localhost';
GRANT ALL PRIVILEGES ON productdb.* TO 'urbanvogue'@'localhost';
GRANT ALL PRIVILEGES ON urbanvogue_inventory.* TO 'urbanvogue'@'localhost';
GRANT ALL PRIVILEGES ON urbanvogue_order.* TO 'urbanvogue'@'localhost';
GRANT ALL PRIVILEGES ON urbanvogue_payment.* TO 'urbanvogue'@'localhost';
FLUSH PRIVILEGES;

-- Verify databases
SHOW DATABASES;
```

### Step 3: Build Backend Services

```powershell
cd C:\Users\mithunhari.k\Desktop\urbanvogue

# Clean and build all services
mvn clean install -DskipTests

# This may take 5-10 minutes on first run (downloading dependencies)

# Verify build success
# You should see "BUILD SUCCESS" messages for each service
```

**Build individual services (optional):**
```powershell
# Build specific service
cd api-gateway
mvn clean install -DskipTests

cd ..\auth-service
mvn clean install -DskipTests

cd ..\product-service
mvn clean install -DskipTests

cd ..\inventory-service
mvn clean install -DskipTests

cd ..\order-service
mvn clean install -DskipTests

cd ..\payment-service
mvn clean install -DskipTests

cd ..\notification-service
mvn clean install -DskipTests
```

### Step 4: Configure Environment Variables (Optional but Recommended)

**Create `.env` file in project root:**
```powershell
# Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_USER=root
DB_PASSWORD=your_mysql_password

# RabbitMQ Configuration
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest

# JWT Configuration
JWT_SECRET=your_super_secret_jwt_key_that_is_at_least_256_bits_long_for_HS256_algorithm
JWT_EXPIRY=900000

# Stripe Configuration (for payment integration)
STRIPE_API_KEY=sk_test_your_stripe_test_key_here
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret_here

# Email Configuration (for Gmail SMTP)
GMAIL_USERNAME=your_gmail@gmail.com
GMAIL_PASSWORD=your_gmail_app_password
```

### Step 5: Start Backend Services

**Open 7 PowerShell terminals** and run the following commands (one in each terminal):

**Terminal 1 - API Gateway:**
```powershell
cd C:\Users\mithunhari.k\Desktop\urbanvogue\api-gateway
mvn spring-boot:run
# Should start on port 8080
```

**Terminal 2 - Auth Service:**
```powershell
cd C:\Users\mithunhari.k\Desktop\urbanvogue\auth-service
mvn spring-boot:run
# Should start on port 8082
```

**Terminal 3 - Product Service:**
```powershell
cd C:\Users\mithunhari.k\Desktop\urbanvogue\product-service
mvn spring-boot:run
# Should start on port 8083
```

**Terminal 4 - Inventory Service:**
```powershell
cd C:\Users\mithunhari.k\Desktop\urbanvogue\inventory-service
mvn spring-boot:run
# Should start on port 8086
```

**Terminal 5 - Order Service:**
```powershell
cd C:\Users\mithunhari.k\Desktop\urbanvogue\order-service
mvn spring-boot:run
# Should start on port 8085
```

**Terminal 6 - Payment Service:**
```powershell
cd C:\Users\mithunhari.k\Desktop\urbanvogue\payment-service
mvn spring-boot:run
# Should start on port 8087
```

**Terminal 7 - Notification Service:**
```powershell
cd C:\Users\mithunhari.k\Desktop\urbanvogue\notification-service
mvn spring-boot:run
# Should start on port 8088
```

**All services started successfully when you see:**
```
Started [ServiceName]Application in X.XXX seconds
```

### Step 6: Start Frontend

**Open a new PowerShell terminal:**
```powershell
cd C:\Users\mithunhari.k\Desktop\urbanvogue\frontend

# Install dependencies (first time only)
npm install

# Start development server
npm run dev

# Should output:
#   Local:   http://localhost:5173/
#   Press q to quit
```

### Step 7: Verify Everything is Running

**Check All Services are Healthy:**

```powershell
# Health check for each service
Invoke-WebRequest http://localhost:8080/actuator/health
Invoke-WebRequest http://localhost:8082/actuator/health
Invoke-WebRequest http://localhost:8083/actuator/health
Invoke-WebRequest http://localhost:8085/actuator/health
Invoke-WebRequest http://localhost:8086/actuator/health
Invoke-WebRequest http://localhost:8087/actuator/health
Invoke-WebRequest http://localhost:8088/actuator/health

# All should return status "UP"
```

**Access the Application:**

- **Frontend:** http://localhost:5173
- **API Gateway:** http://localhost:8080/api
- **RabbitMQ Management:** http://localhost:15672 (guest/guest)
- **Zipkin:** http://localhost:9411

---

## Testing the Application

### Run Unit Tests

```powershell
cd C:\Users\mithunhari.k\Desktop\urbanvogue

# Run all tests across all services
mvn clean test

# Run tests for specific service
mvn clean test -pl order-service

# Run specific test class
mvn clean test -Dtest=OrderServiceTest
```

### Run Integration Tests

```powershell
# Run integration tests
mvn clean verify -pl order-service

# Tests will use H2 in-memory database for speed
```

### Run Performance Tests

```powershell
# Run performance test class
mvn clean test -Dtest=OrderServicePerformanceTest -pl order-service

# Output will show:
# PERF-01: Single order creation time
# PERF-02: Sequential orders throughput
# PERF-03: Concurrent orders stress test
# PERF-04: Order lookups performance
```

### Manual API Testing with Postman

**Import Collection:**
1. Download Postman from https://www.postman.com
2. Import the API collection (if provided)
3. Or manually create requests:

**1. Register User:**
```
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "Password@123"
}
```

**2. Login:**
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "email": "test@example.com",
  "password": "Password@123"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {...}
}
# Copy the token for authenticated requests
```

**3. Get Products:**
```
GET http://localhost:8080/api/products
Authorization: Bearer <YOUR_TOKEN>
```

**4. Create Product (Admin):**
```
POST http://localhost:8080/api/products
Authorization: Bearer <YOUR_TOKEN>
Content-Type: application/json

{
  "name": "Urban Sneaker Pro",
  "description": "Premium athletic footwear",
  "price": 129.99,
  "imageUrl": "https://example.com/sneaker.jpg",
  "category": "Footwear"
}
```

**5. Create Order:**
```
POST http://localhost:8080/api/orders
Authorization: Bearer <YOUR_TOKEN>
Content-Type: application/json

{
  "items": [
    {
      "productId": 1,
      "quantity": 2
    }
  ]
}
```

### Test Using PowerShell Script

**Create test_api.ps1:**
```powershell
# Test API endpoints
$apiGateway = "http://localhost:8080"

# Test health endpoint
Write-Host "Testing API Gateway Health..."
$health = Invoke-WebRequest -Uri "$apiGateway/actuator/health"
Write-Host "Status: $($health.StatusCode)"

# Test products endpoint
Write-Host "`nTesting Products Endpoint..."
$products = Invoke-WebRequest -Uri "$apiGateway/api/products"
Write-Host "Status: $($products.StatusCode)"
Write-Host "Response: $($products.Content)"
```

**Run the test script:**
```powershell
cd C:\Users\mithunhari.k\Desktop\urbanvogue
powershell.exe -ExecutionPolicy Bypass -File test_api.ps1
```

---

## Common Issues & Troubleshooting

### Issue 1: MySQL Connection Error

**Error:** `Could not connect to database at localhost:3306`

**Solution:**
```powershell
# Check if MySQL service is running
Get-Service MySQL80

# Start MySQL service if stopped
Start-Service MySQL80

# Verify MySQL is listening
netstat -an | findstr :3306

# Check MySQL credentials in application.yml
# Ensure username and password are correct
```

### Issue 2: RabbitMQ Connection Error

**Error:** `Error connecting to RabbitMQ broker`

**Solution:**
```powershell
# Check if RabbitMQ service is running
Get-Service RabbitMQ

# Start RabbitMQ service if stopped
Start-Service RabbitMQ

# Verify RabbitMQ is running
Get-Process rabbitmq

# Check RabbitMQ management console
# http://localhost:15672
# Username: guest
# Password: guest
```

### Issue 3: Port Already in Use

**Error:** `Address already in use: bind`

**Solution:**
```powershell
# Find process using port 8080 (example)
netstat -ano | findstr :8080

# Kill the process
# taskkill /PID <process_id> /F

# Or use a different port by adding to application.yml
# server:
#   port: 8081

# Or change the port in the service configuration
$env:SERVER_PORT = 8081
```

### Issue 4: JWT Token Validation Error

**Error:** `Invalid JWT token`

**Solution:**
```powershell
# Check JWT_SECRET is set and consistent across services
# Update application.yml with same secret key in all services

# Regenerate token by logging in again
# POST http://localhost:8080/api/auth/login

# Verify token in new requests
# Header: Authorization: Bearer <NEW_TOKEN>
```

### Issue 5: Service Startup Takes Too Long

**Issue:** Service takes > 30 seconds to start

**Solution:**
```powershell
# This is normal for first startup
# Services download dependencies and build entities

# To speed up subsequent builds:
# Use spring-boot:run instead of full mvn run

# Or build once and run JAR:
cd auth-service
mvn clean package -DskipTests
java -jar target/auth-service-0.0.1-SNAPSHOT.jar
```

### Issue 6: Frontend Cannot Connect to Backend

**Error:** `CORS error` or `Failed to fetch`

**Solution:**
```javascript
// Check vite.config.js has correct proxy configuration
// Verify API Gateway is running on http://localhost:8080
// Check console in browser (F12) for exact error

// If CORS error:
// - Ensure application.yml has CORS configuration
// - Check if API Gateway is filtering requests

// If connection refused:
// - Verify backend services are running
// - Check port numbers: API Gateway should be 8080
```

---

## Project Structure

```
urbanvogue/
├── PROJECT_REPORT.md                 # Main comprehensive report
├── EXECUTIVE_SUMMARY.md              # Executive summary for quick review
├── TECHNICAL_SPECIFICATIONS.md       # Detailed technical specs
├── QUICK_START.md                    # This file
│
├── api-gateway/                      # API Gateway Service
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/urbanvogue/api_gateway/
│   │   │   │   ├── config/
│   │   │   │   ├── filter/           # JWT filter
│   │   │   │   └── ApiGatewayApplication.java
│   │   │   └── resources/
│   │   │       └── application.yml   # Gateway configuration
│   │   └── test/
│   └── pom.xml                       # Maven configuration
│
├── auth-service/                     # Authentication Service (8082)
│   ├── src/main/java/com/urbanvogue/auth_service/
│   │   ├── controller/               # REST endpoints
│   │   ├── service/                  # Business logic
│   │   ├── repository/               # Database layer
│   │   ├── model/                    # Entity classes
│   │   ├── dto/                      # Data transfer objects
│   │   ├── security/                 # JWT configuration
│   │   └── AuthServiceApplication.java
│   ├── frontend/                     # Optional frontend
│   ├── pom.xml
│   └── test_result.log               # Test execution results
│
├── product-service/                  # Product Service (8083)
│   ├── src/main/java/com/urbanvogue/product_service/
│   │   ├── controller/               # REST endpoints
│   │   ├── service/                  # Business logic
│   │   ├── repository/               # Database layer
│   │   ├── model/                    # Entity classes
│   │   ├── dto/                      # Data transfer objects
│   │   └── ProductServiceApplication.java
│   ├── pom.xml
│   └── test_result.log
│
├── inventory-service/                # Inventory Service (8086)
│   ├── src/main/java/com/urbanvogue/inventory_service/
│   ├── pom.xml
│   └── perf_result.log
│
├── order-service/                    # Order Service (8085)
│   ├── src/main/java/com/urbanvogue/order_service/
│   │   ├── controller/
│   │   ├── service/                  # OrderService with saga logic
│   │   ├── client/                   # Feign & WebClient
│   │   ├── messaging/                # Event listeners
│   │   ├── model/                    # Order entities
│   │   ├── dto/
│   │   └── OrderServiceApplication.java
│   ├── src/test/java/
│   │   └── OrderServicePerformanceTest.java  # Performance tests
│   ├── pom.xml
│   └── perf_result.log
│
├── payment-service/                  # Payment Service (8087)
│   ├── src/main/java/com/urbanvogue/payment_service/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── messaging/                # Event publishing
│   │   ├── model/
│   │   └── stripe/                   # Stripe integration
│   ├── pom.xml
│   └── test_result.log
│
├── notification-service/             # Notification Service (8088)
│   ├── src/main/java/com/urbanvogue/notification_service/
│   │   ├── messaging/                # Event listeners
│   │   ├── service/                  # Email service
│   │   ├── config/                   # RabbitMQ config
│   │   ├── dto/
│   │   └── NotificationServiceApplication.java
│   ├── src/files.txt                 # List of files
│   ├── build.log
│   └── pom.xml
│
├── frontend/                         # React Frontend
│   ├── src/
│   │   ├── pages/                    # React page components
│   │   │   ├── HomePage.jsx
│   │   │   ├── LoginPage.jsx
│   │   │   ├── RegisterPage.jsx
│   │   │   ├── ProductsPage.jsx
│   │   │   ├── ProductDetailPage.jsx
│   │   │   ├── CartPage.jsx
│   │   │   ├── CheckoutPage.jsx
│   │   │   ├── PaymentSuccessPage.jsx
│   │   │   ├── OrdersPage.jsx
│   │   │   ├── OrderDetailPage.jsx
│   │   │   └── DashboardPage.jsx
│   │   ├── components/               # Reusable components
│   │   │   ├── Navbar.jsx
│   │   │   ├── Footer.jsx
│   │   │   ├── ProductCard.jsx
│   │   │   ├── CartItem.jsx
│   │   │   ├── PrivateRoute.jsx
│   │   │   └── InputField.jsx
│   │   ├── api/                      # API client modules
│   │   │   ├── authApi.js
│   │   │   ├── productApi.js
│   │   │   ├── orderApi.js
│   │   │   ├── paymentApi.js
│   │   │   └── inventoryApi.js
│   │   ├── context/                  # React Context
│   │   │   ├── AuthContext.jsx
│   │   │   └── CartContext.jsx
│   │   ├── hooks/                    # Custom hooks
│   │   │   ├── useAuth.js
│   │   │   └── useCart.js
│   │   ├── App.jsx                   # Main app component
│   │   ├── main.jsx                  # Entry point
│   │   └── index.css                 # Global styles
│   ├── package.json                  # Dependencies
│   ├── vite.config.js                # Vite configuration
│   └── index.html
│
├── db_schema.txt                     # Database schema dump
├── db_schema_utf8.txt               # Database schema (UTF-8)
├── architecture_diagram.md           # Mermaid architecture diagram
│
├── test_*.json                       # Test data files
├── test-e2e.ps1                      # E2E test script
├── status.txt                        # Project status
└── README.md                         # Main readme
```

---

## Key Files Reference

### Configuration Files to Review

1. **api-gateway/src/main/resources/application.yml**
   - Gateway port and routes
   - JWT validation setup

2. **[service-name]/src/main/resources/application.yml**
   - Database configuration
   - RabbitMQ configuration
   - Logging setup

3. **frontend/vite.config.js**
   - API proxy configuration
   - Build settings

### Important Source Files

1. **Order Service (Core Orchestration)**
   - `OrderService.java` - Main order creation logic with saga pattern
   - `OrderServicePerformanceTest.java` - Performance benchmarks

2. **Payment Service (Integration)**
   - `PaymentService.java` - Stripe integration
   - `WebhookController.java` - Webhook handling

3. **Frontend (User Interface)**
   - `App.jsx` - Main application component
   - `CheckoutPage.jsx` - Payment flow
   - `authApi.js` - API communication

### Test Result Files

- `product-test-result.txt` - Product service test execution
- `order-test-result.txt` - Order service test execution
- `payment-test-result.txt` - Payment service test execution
- `inventory-test-result.txt` - Inventory service test execution

---

## Quick Command Reference

```powershell
# Build
mvn clean install -DskipTests

# Run tests
mvn clean test

# Run single service
cd <service-name> && mvn spring-boot:run

# Run frontend
cd frontend && npm install && npm run dev

# Check health
curl http://localhost:8080/actuator/health

# View logs
Get-Content .\*.log -Tail 50

# Kill process on port
netstat -ano | findstr :8080
taskkill /PID <process_id> /F
```

---

## Next Steps After Setup

1. **Explore the API** - Use Postman or curl to test endpoints
2. **Browse Frontend** - Navigate through http://localhost:5173
3. **Review Logs** - Check service logs in their respective terminals
4. **Run Performance Tests** - Execute performance benchmarks
5. **Read Reports** - Review PROJECT_REPORT.md for detailed information
6. **Check Monitoring** - Visit http://localhost:9411 (Zipkin) for traces

---

**Setup Complete!** 🎉

You now have a fully functional microservices e-commerce platform running locally.

For detailed information, refer to PROJECT_REPORT.md  
For technical details, refer to TECHNICAL_SPECIFICATIONS.md  
For executive overview, refer to EXECUTIVE_SUMMARY.md


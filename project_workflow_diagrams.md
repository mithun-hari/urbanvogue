# UrbanVogue Microservices Workflows

This document visualizes the complete end-to-end workflows of the core operations in the UrbanVogue e-commerce architecture using Mermaid sequence diagrams.

## 1. User Authentication (Registration & Login)
This flow demonstrates how a user creates an account and authenticates to receive a JSON Web Token (JWT).

```mermaid
sequenceDiagram
    participant Client
    participant API_Gateway
    participant Auth_Service
    participant Database as users Database

    %% Registration Flow
    Client->>API_Gateway: POST /api/auth/register
    API_Gateway->>Auth_Service: Route Request
    Auth_Service->>Database: Insert new User (Hashed Password)
    Database-->>Auth_Service: Success
    Auth_Service-->>Client: Registration Complete

    %% Login Flow
    Client->>API_Gateway: POST /api/auth/login
    API_Gateway->>Auth_Service: Route Request
    Auth_Service->>Database: Verify Credentials
    Database-->>Auth_Service: User Validated
    Auth_Service->>Auth_Service: Generate JWT
    Auth_Service-->>Client: Return JWT Token
```

---

## 2. Product Browsing
This flow demonstrates how a user views the product catalog. Only the Product Service is involved here.

```mermaid
sequenceDiagram
    participant Client
    participant API_Gateway
    participant Product_Service
    participant Database as product Database

    Client->>API_Gateway: GET /api/products
    API_Gateway->>Product_Service: Route Request
    Product_Service->>Database: Fetch Product Catalog
    Database-->>Product_Service: List of Products
    Product_Service-->>Client: Return Products Response
```

---

## 3. Order Placement Workflow
This is the core transaction. The Order Service acts as an orchestrator, synchronously communicating with Product and Inventory services via OpenFeign.

```mermaid
sequenceDiagram
    participant Client
    participant API_Gateway
    participant Order_Service
    participant Product_Service
    participant Inventory_Service
    participant Database as orders Database

    Client->>API_Gateway: POST /orders (Provides JWT)
    API_Gateway->>Order_Service: Route Request
    
    %% Synchronous Feign Calls
    Order_Service->>Product_Service: Fetch Product Details via Feign
    Product_Service-->>Order_Service: Returns Details (Price validation)
    
    Order_Service->>Inventory_Service: Check/Reserve Stock via Feign
    Inventory_Service-->>Order_Service: Stock Reserved Successfully
    
    %% Database Transaction
    Order_Service->>Database: Save Order (Status: PENDING)
    Database-->>Order_Service: Order Saved
    
    Order_Service-->>Client: Order Created Response (Returns Order ID)
```

---

## 4. Payment Processing & Notification Workflow
After an order is PENDING, the user initiates payment. The Payment Service orchestrates the transaction, updates the order status, and triggers the email receipt.

```mermaid
sequenceDiagram
    participant Client
    participant API_Gateway
    participant Payment_Service
    participant Order_Service
    participant Notification_Service
    participant ThirdParty as Stripe / Gateway

    Client->>API_Gateway: POST /payments (Provides Order ID)
    API_Gateway->>Payment_Service: Route Request
    
    %% External API Call
    Payment_Service->>ThirdParty: Process Payment Transaction
    ThirdParty-->>Payment_Service: Payment Success Webhook
    
    %% Synchronous Feign Calls
    Payment_Service->>Order_Service: Update Status to COMPLETED via Feign
    Order_Service-->>Payment_Service: Status Updated Acknowledgment
    
    Payment_Service->>Notification_Service: Send Email Receipt Request via Feign
    
    %% External API Call (SMTP)
    Notification_Service->>Client: Send Physical SMTP Email to User
    
    Payment_Service-->>Client: Payment Confirmation Response
```

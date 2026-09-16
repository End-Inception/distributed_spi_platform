# Data Model

## Distributed High-Scale API Gateway & Event Processing Platform

**Version:** 1.0  
**Status:** Initial Data Model  
**Date:** September 2026

---

# 1. Overview

The platform will initially use PostgreSQL as its primary relational database.

The initial transactional entities are:

```text
User
Product
Order
OrderItem
```

The database will eventually be optimized using:

- Indexing
- Query optimization
- Connection pooling
- Read replicas
- Partitioning
- Sharding

---

# 2. Entity Relationship

The initial relationship model is:

```text
             +-------------+
             |    USER     |
             +------+------+
                    |
                    | 1
                    |
                    | N
             +------v------+
             |    ORDER    |
             +------+------+
                    |
                    | 1
                    |
                    | N
             +------v------+
             | ORDER_ITEM  |
             +------+------+
                    |
                    | N
                    |
                    | 1
             +------v------+
             |   PRODUCT   |
             +-------------+
```

---

# 3. User Table

```sql
users
```

| Column | Type | Description |
|---|---|---|
| id | UUID | Primary key |
| name | VARCHAR | User name |
| email | VARCHAR | Unique email |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

Primary key:

```text
id
```

Unique constraint:

```text
email
```

Index:

```text
idx_users_email
```

---

# 4. Product Table

```sql
products
```

| Column | Type | Description |
|---|---|---|
| id | UUID | Primary key |
| name | VARCHAR | Product name |
| description | TEXT | Product description |
| price | DECIMAL | Product price |
| stock | INTEGER | Available inventory |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

Primary key:

```text
id
```

Potential indexes:

```text
idx_products_name
idx_products_created_at
```

Product GET requests are expected to be heavily cached using Redis.

---

# 5. Order Table

```sql
orders
```

| Column | Type | Description |
|---|---|---|
| id | UUID | Primary key |
| user_id | UUID | User placing order |
| status | VARCHAR | Current order status |
| total_amount | DECIMAL | Total order value |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

Foreign key:

```text
user_id → users.id
```

Potential index:

```text
idx_orders_user_id
```

---

# 6. Order Item Table

```sql
order_items
```

| Column | Type | Description |
|---|---|---|
| id | UUID | Primary key |
| order_id | UUID | Associated order |
| product_id | UUID | Associated product |
| quantity | INTEGER | Quantity purchased |
| unit_price | DECIMAL | Product price at purchase time |

Foreign keys:

```text
order_id → orders.id

product_id → products.id
```

Indexes:

```text
idx_order_items_order_id
idx_order_items_product_id
```

---

# 7. Entity Relationships

## User → Orders

One user can have multiple orders.

```text
User 1 ---- N Orders
```

---

## Order → Order Items

One order can contain multiple items.

```text
Order 1 ---- N OrderItems
```

---

## Product → Order Items

One product can appear in many order items.

```text
Product 1 ---- N OrderItems
```

---

# 8. Initial SQL Schema

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(12,2) NOT NULL,
    stock INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(12,2) NOT NULL,

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES orders(id),

    CONSTRAINT fk_order_items_product
        FOREIGN KEY (product_id)
        REFERENCES products(id)
);
```

---

# 9. Index Strategy

Initial indexes:

```sql
CREATE INDEX idx_users_email
ON users(email);

CREATE INDEX idx_orders_user_id
ON orders(user_id);

CREATE INDEX idx_order_items_order_id
ON order_items(order_id);

CREATE INDEX idx_order_items_product_id
ON order_items(product_id);
```

Indexes will be added based on actual query patterns rather than indiscriminately indexing every column.

---

# 10. High-Scale Considerations

The system is expected to eventually process a large number of requests.

Therefore, the database design must consider:

### Connection Pooling

Application instances should reuse database connections instead of creating a new connection for every request.

### Read Replicas

Read-heavy workloads can eventually be distributed across read replicas.

```text
                Application
                     |
            +--------+--------+
            |                 |
            v                 v
         Primary          Read Replicas
          WRITE               READ
```

### Partitioning

Large tables such as `orders` may eventually be partitioned based on an appropriate key such as time.

Example:

```text
orders
  |
  +-- 2026-Q1
  +-- 2026-Q2
  +-- 2026-Q3
  +-- 2026-Q4
```

The actual partitioning strategy will be determined after measuring query patterns.

### Sharding

If a single database cluster becomes insufficient, data may eventually be horizontally partitioned across shards.

Example:

```text
Hash(user_id)
      |
      +----> Shard 1
      |
      +----> Shard 2
      |
      +----> Shard 3
      |
      +----> Shard 4
```

Sharding will not be implemented until a measurable requirement exists.

---

# 11. Redis Data Model

Redis will be used for data that benefits from low-latency access.

Example cache key:

```text
product:{productId}
```

Example:

```text
product:12345
```

Value:

```json
{
  "id": "12345",
  "name": "Laptop",
  "price": 75000,
  "stock": 100
}
```

---

# 12. Rate Limit Keys

Example:

```text
rate_limit:{userId}
```

Example:

```text
rate_limit:user_12345
```

The exact data structure will depend on the selected rate-limiting algorithm.

---

# 13. Idempotency Keys

Example:

```text
idempotency:{key}
```

Example:

```text
idempotency:550e8400-e29b-41d4-a716-446655440000
```

The key will prevent duplicate processing of retried operations.

---

# 14. Kafka Event Model

Kafka will not replace PostgreSQL as the source of truth for transactional data.

Kafka will carry events generated by services.

Example:

```json
{
  "eventId": "evt_12345",
  "eventType": "ORDER_CREATED",
  "version": 1,
  "timestamp": "2026-09-16T10:00:00Z",
  "payload": {
    "orderId": "ord_12345",
    "userId": "usr_12345"
  }
}
```

---

# 15. Initial Kafka Topics

Potential topics:

```text
orders.created
orders.updated
orders.cancelled
payments.completed
inventory.updated
notifications.requested
```

The exact topic structure will evolve as the event-driven architecture is implemented.

---

# 16. Data Ownership

Each service should own its transactional data as the architecture evolves.

Initial conceptual ownership:

```text
User Service
    |
    +---- Users

Product Service
    |
    +---- Products

Order Service
    |
    +---- Orders
    +---- Order Items
```

Services should communicate through APIs or events rather than directly modifying another service's data.

---

# 17. Consistency

The transactional database will provide strong consistency for operations that require it.

Kafka-based asynchronous workflows will introduce eventual consistency between independently processed operations.

Example:

```text
Order Created
     |
     v
Kafka
     |
     +----> Payment
     |
     +----> Inventory
     |
     +----> Notification
```

The order may temporarily have:

```text
status = PROCESSING
```

until downstream operations complete.

---

# 18. Future Data Architecture

The database architecture may evolve toward:

```text
                    Application
                         |
                 +-------+-------+
                 |               |
                 v               v
             Redis Cache     API Services
                                 |
                    +------------+------------+
                    |                         |
                    v                         v
                 Primary                Read Replicas
                    |
                    v
               Partitioning
                    |
                    v
                 Sharding
```

This evolution will be driven by actual workload measurements.

---

# 19. Data Model Evolution

The schema is expected to change as the project evolves.

Future considerations include:

- Soft deletion
- Audit fields
- Optimistic locking
- Database partitioning
- Sharding keys
- Event sourcing where appropriate
- Data retention
- Archival
- Distributed transactions
- Outbox pattern

These will be introduced only when required by the architecture.

---

# 20. Current Status

```text
[✓] Initial entities identified
[✓] Relationships defined
[✓] Initial PostgreSQL schema
[✓] Initial indexes
[✓] Redis key strategy
[✓] Initial Kafka event model
[ ] Implement database
[ ] Add migrations
[ ] Implement repositories
[ ] Benchmark queries
[ ] Optimize indexes
[ ] Add Redis
[ ] Add Kafka
[ ] Evaluate read replicas
[ ] Evaluate partitioning
[ ] Evaluate sharding
```

---

# 21. Next Step

After completing the API specification and data model, the project will move to:

**Phase 1 — Backend Project Initialization**

The next implementation steps will be:

1. Create the Java 21 + Spring Boot project.
2. Configure Maven.
3. Configure PostgreSQL.
4. Configure application profiles.
5. Create the package structure.
6. Create the User entity.
7. Create the User repository.
8. Implement the first REST endpoint.
9. Add validation and exception handling.
10. Write unit/integration tests.
11. Run the first load test against the basic implementation.
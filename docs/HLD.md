# High-Level Design (HLD)

## Distributed High-Scale API Gateway & Event Processing Platform

**Version:** 1.0  
**Status:** Initial Architecture  
**Date:** September 2026

---

# 1. Overview

The **Distributed High-Scale API Gateway & Event Processing Platform** is a production-style distributed system designed to handle very high volumes of API traffic while maintaining low latency, scalability, availability, and fault tolerance.

The platform acts as an intermediary between clients and backend services.

Instead of clients directly communicating with individual backend services, all requests enter through a scalable API Gateway layer.

The system will progressively evolve from a simple monolithic backend into a distributed architecture consisting of:

- API Gateway
- Load Balancer
- Redis
- Kafka
- Backend Services
- PostgreSQL
- Read Replicas
- Containerized Services
- Kubernetes
- Autoscaling
- Observability
- Load Testing
- Multi-region deployment

The system will be built incrementally. Each architectural improvement will be benchmarked to identify its impact on throughput, latency, resource utilization, and reliability.

---

# 2. Problem Statement

Modern applications can receive extremely large numbers of API requests during normal operation and sudden traffic spikes.

A simple architecture such as:

```text
Client
   |
   v
Backend Server
   |
   v
Database
```

can become a bottleneck as traffic increases.

Potential problems include:

- Server resource exhaustion
- Database overload
- High request latency
- Excessive database reads
- Uneven traffic distribution
- Lack of fault tolerance
- Cascading service failures
- Duplicate requests
- Slow downstream services
- Difficulty scaling horizontally
- Difficulty processing large asynchronous workloads
- Lack of monitoring and observability

The goal of this project is to design and implement a distributed system that addresses these problems using scalable and fault-tolerant architecture patterns.

The system should be capable of scaling horizontally as traffic increases.

The actual throughput of the implementation will be measured through load testing rather than assumed.

---

# 3. Goals

## 3.1 Primary Goals

The system should provide:

1. Horizontally scalable API handling.
2. Centralized API request routing.
3. Distributed rate limiting.
4. Low-latency caching.
5. Asynchronous event processing.
6. Reliable communication between services.
7. Database scalability.
8. Fault tolerance.
9. Automatic scaling.
10. Monitoring and observability.
11. Performance benchmarking.
12. Cloud deployment.

---

# 4. Non-Goals

The project will not initially attempt to:

- Build a complete commercial API management platform.
- Implement every possible microservice.
- Support every authentication protocol.
- Build a custom database engine.
- Build a custom message broker.
- Guarantee a specific global requests-per-second number without measurement.
- Deploy a production-scale cloud infrastructure from the beginning.

The architecture will evolve gradually based on measurable requirements.

---

# 5. Functional Requirements

## 5.1 API Gateway

The API Gateway should:

- Accept incoming HTTP requests.
- Authenticate clients.
- Validate requests.
- Apply rate limits.
- Route requests to backend services.
- Provide caching where appropriate.
- Generate request IDs.
- Produce logs and metrics.
- Handle errors consistently.

---

## 5.2 Authentication

The platform should support authenticated API requests.

Example:

```http
GET /api/v1/orders/123

Authorization: Bearer <JWT>
```

The Gateway will validate the authentication information before forwarding the request.

---

## 5.3 Rate Limiting

The platform should limit the number of requests a client can make during a specified period.

Example:

```text
Free User:
100 requests / second

Premium User:
10,000 requests / second
```

Rate limits should remain consistent even when requests are distributed across multiple Gateway instances.

---

## 5.4 Caching

Frequently accessed data should be cached to reduce database load.

Example:

```text
GET /api/v1/products/123
```

The system should first check the cache.

```text
Request
   |
   v
Redis
   |
   +---- HIT ----> Response
   |
   +---- MISS ---> Database
                     |
                     v
                   Redis
```

---

## 5.5 Order Processing

The platform should support order creation.

Example:

```http
POST /api/v1/orders
```

Request:

```json
{
  "userId": 123,
  "productId": 456,
  "quantity": 2
}
```

The system should persist the order and publish an event for asynchronous processing.

---

## 5.6 Event Processing

Events such as:

```text
ORDER_CREATED
PAYMENT_COMPLETED
INVENTORY_UPDATED
ORDER_CANCELLED
```

should be processed asynchronously using Kafka.

Multiple consumers should be able to process the same event stream for different purposes.

---

## 5.7 Observability

The system should provide metrics for:

- Request rate
- Request latency
- Error rate
- Cache hit ratio
- Database latency
- Redis latency
- Kafka consumer lag
- CPU utilization
- Memory utilization
- Number of active instances

---

# 6. Non-Functional Requirements

## 6.1 Scalability

The system should support horizontal scaling.

Instead of relying on one increasingly powerful server:

```text
Server
  |
  v
Bigger Server
  |
  v
Even Bigger Server
```

the architecture should support:

```text
Instance 1
Instance 2
Instance 3
...
Instance N
```

Additional instances should be added as traffic increases.

---

## 6.2 Availability

The system should continue serving requests when individual application instances fail.

Example:

```text
Gateway 1   ❌
Gateway 2   ✅
Gateway 3   ✅
```

The load balancer should stop sending traffic to unhealthy instances.

---

## 6.3 Performance

Initial performance targets:

```text
P50 < 50 ms
P95 < 150 ms
P99 < 300 ms
```

These values are engineering targets and will be validated through load testing.

---

## 6.4 Fault Tolerance

The system should tolerate failures involving:

- Application instances
- Redis
- Kafka consumers
- Backend services
- Database replicas
- Network communication

Mechanisms will include:

- Timeouts
- Retries
- Exponential backoff
- Circuit breakers
- Health checks
- Idempotency
- Dead-letter queues
- Backpressure

---

## 6.5 Maintainability

Services should have clear responsibilities and well-defined interfaces.

The system should avoid unnecessary microservices and excessive architectural complexity.

---

# 7. High-Level Architecture

The initial target architecture is:

```text
                              CLIENTS
                                 |
                                 v
                         +---------------+
                         |      CDN      |
                         +-------+-------+
                                 |
                                 v
                         +---------------+
                         | Load Balancer |
                         +-------+-------+
                                 |
                +----------------+----------------+
                |                |                |
                v                v                v
          +-----------+    +-----------+    +-----------+
          | Gateway 1 |    | Gateway 2 |    | Gateway N |
          +-----+-----+    +-----+-----+    +-----+-----+
                |                |                |
                +----------------+----------------+
                                 |
                    +------------+------------+
                    |                         |
                    v                         v
             +-------------+           +-------------+
             |    Redis    |           | Auth Service|
             |   Cluster   |           +-------------+
             +------+------+ 
                    |
                    v
             +-------------+
             | API Services|
             +------+------+ 
                    |
          +---------+---------+
          |         |         |
          v         v         v
      +-------+ +-------+ +-------+
      | User  | |Product| | Order |
      |Service| |Service| |Service|
      +---+---+ +---+---+ +---+---+
          |         |         |
          |         |         v
          |         |    +----------+
          |         |    |  Kafka   |
          |         |    | Cluster  |
          |         |    +----+-----+
          |         |         |
          |         |    +----+-----+----------+
          |         |    |          |          |
          |         |    v          v          v
          |         | Payment    Notification Analytics
          |         | Consumer     Worker      Consumer
          |         |
          +---------+-------------+
                    |
                    v
             +-------------+
             | PostgreSQL  |
             +------+------+
                    |
              +-----+-----+
              |           |
              v           v
         Primary DB   Read Replicas
```

---

# 8. Component Responsibilities

## 8.1 CDN

Responsibilities:

- Serve cacheable content from edge locations.
- Reduce origin traffic.
- Reduce latency for cacheable resources.

---

## 8.2 Load Balancer

Responsibilities:

- Distribute traffic.
- Perform health checks.
- Remove unhealthy instances.
- Forward traffic to available Gateway instances.

---

## 8.3 API Gateway

The Gateway is the primary entry point for API requests.

Responsibilities:

```text
Authentication
Rate Limiting
Request Validation
Routing
Caching
Logging
Metrics
```

The Gateway should remain stateless wherever possible.

---

## 8.4 Redis

Redis will provide low-latency distributed state.

Initial use cases:

```text
Caching
Rate Limiting
Idempotency
Session Information
Distributed Coordination
```

---

## 8.5 User Service

Responsible for:

- User creation
- User retrieval
- User management

Example APIs:

```http
POST /api/v1/users
GET /api/v1/users/{id}
```

---

## 8.6 Product Service

Responsible for:

- Product creation
- Product retrieval
- Product updates
- Product information

Example:

```http
GET /api/v1/products/{id}
```

Product information is a good candidate for caching.

---

## 8.7 Order Service

Responsible for:

- Creating orders
- Retrieving orders
- Updating order state
- Publishing order events

Example:

```http
POST /api/v1/orders
GET /api/v1/orders/{id}
```

---

## 8.8 Kafka

Kafka will act as the asynchronous event backbone.

Example:

```text
ORDER_CREATED
      |
      v
    Kafka
      |
      +----> Payment Consumer
      |
      +----> Inventory Consumer
      |
      +----> Notification Consumer
      |
      +----> Analytics Consumer
```

Kafka allows producers and consumers to operate independently.

---

## 8.9 PostgreSQL

PostgreSQL will provide durable relational storage.

Initial architecture:

```text
Application
     |
     v
PostgreSQL
```

The architecture will later evolve toward:

```text
              Application
                   |
          +--------+--------+
          |                 |
          v                 v
       Primary         Read Replicas
```

Further scaling techniques such as partitioning and sharding will be investigated after measuring actual bottlenecks.

---

# 9. Request Flow

## 9.1 Read Request

Example:

```http
GET /api/v1/products/123
```

Flow:

```text
Client
  |
  v
CDN
  |
  v
Load Balancer
  |
  v
API Gateway
  |
  +--> Authentication
  |
  +--> Rate Limiting
  |
  +--> Redis Cache
          |
          +---- HIT ---> Response
          |
          +---- MISS
                 |
                 v
            Product Service
                 |
                 v
             PostgreSQL
                 |
                 v
               Redis
                 |
                 v
              Response
```

---

# 10. Write Request Flow

Example:

```http
POST /api/v1/orders
```

Flow:

```text
Client
  |
  v
Load Balancer
  |
  v
API Gateway
  |
  +--> Authentication
  |
  +--> Rate Limiting
  |
  v
Order Service
  |
  +--> PostgreSQL
  |
  v
Kafka
  |
  +--------+---------+---------+
  |        |         |         |
  v        v         v         v
Payment  Inventory  Email   Analytics
```

The synchronous path should remain as short as practical.

Independent processing should be moved to asynchronous consumers.

---

# 11. Rate Limiting Architecture

The rate limiter will be distributed across Gateway instances.

```text
                    Redis
                      |
             +--------+--------+
             |        |        |
             v        v        v
           GW-1     GW-2     GW-3
             |        |        |
             +--------+--------+
                      |
                    Users
```

Redis will maintain shared rate-limit state.

The initial implementation will use the **Token Bucket** algorithm.

Example:

```text
Capacity = 100 tokens
Refill Rate = 100 tokens/sec

Request
   |
   v
Token Available?
  / \
Yes  No
 |    |
 v    v
Allow Reject
```

---

# 12. Caching Architecture

The project will use the cache-aside pattern.

```text
Application
     |
     v
   Redis
  /     \
HIT     MISS
 |        |
 v        v
Return   Database
           |
           v
         Redis
```

Cache entries will use TTLs.

Example:

```text
product:123
TTL = 300 seconds
```

Potential cache-related problems to investigate:

- Cache stampede
- Hot keys
- Cache penetration
- Cache avalanche
- Cache invalidation

---

# 13. Event-Driven Architecture

The platform will use Kafka for asynchronous events.

Example:

```text
Order Service
      |
      v
ORDER_CREATED
      |
      v
Kafka Topic
      |
      +------------------+
      |                  |
      v                  v
Payment Consumer    Notification Consumer
      |
      v
Payment Processing
```

Kafka topics will be partitioned to support parallel processing.

Consumer groups will allow multiple consumers to process partitions concurrently.

---

# 14. Database Architecture

The initial database:

```text
                PostgreSQL
                    |
        +-----------+-----------+
        |                       |
      Users                  Products
        |                       |
      Orders                  Inventory
```

As traffic increases, optimization will proceed through:

```text
Database
   |
   v
Indexes
   |
   v
Query Optimization
   |
   v
Connection Pooling
   |
   v
Read Replicas
   |
   v
Partitioning
   |
   v
Sharding
```

Each step will only be introduced when justified by measured performance requirements.

---

# 15. Horizontal Scaling

Gateway and backend services should be stateless.

Example:

```text
                    Load Balancer
                   /      |      \
                  v       v       v
                GW-1    GW-2    GW-3
```

If traffic increases:

```text
3 instances
     |
     v
10 instances
     |
     v
20 instances
```

The service should continue operating without requiring changes to application state.

Shared state will be stored in external systems such as Redis or PostgreSQL.

---

# 16. Fault Tolerance

## Gateway Failure

```text
Gateway 1 ❌
Gateway 2 ✅
Gateway 3 ✅
```

The load balancer removes Gateway 1 from rotation.

---

## Backend Service Failure

If a downstream service becomes unavailable:

```text
Gateway
   |
   v
Service
   |
   X
```

the system should use:

```text
Timeout
   |
Retry
   |
Circuit Breaker
```

to prevent cascading failures.

---

## Kafka Consumer Failure

If a consumer crashes:

```text
Kafka
  |
  v
Consumer ❌
```

another consumer in the same consumer group can continue processing the partition after recovery/rebalancing according to Kafka's consumer-group semantics.

---

# 17. Reliability Patterns

The project will implement:

### Timeout

Prevent indefinitely waiting for a downstream service.

### Retry

Retry transient failures.

### Exponential Backoff

Increase delay between retries.

Example:

```text
100 ms
200 ms
400 ms
800 ms
```

### Circuit Breaker

Prevent repeated calls to an unhealthy service.

```text
CLOSED
   |
   | failures
   v
OPEN
   |
   | recovery test
   v
HALF-OPEN
   |
   | success
   v
CLOSED
```

### Idempotency

Ensure retrying a request does not accidentally create duplicate operations.

Example:

```http
Idempotency-Key: 9f8c7d6e
```

### Dead Letter Queue

Messages that repeatedly fail processing can be moved to a dead-letter topic/queue for later investigation.

---

# 18. Observability

The system will expose metrics and traces.

```text
Services
   |
   +---------> Prometheus
   |               |
   |               v
   |            Grafana
   |
   +---------> OpenTelemetry
                   |
                   v
             Distributed Tracing
```

Important metrics:

```text
HTTP Requests/sec
P50 latency
P95 latency
P99 latency
Error rate
CPU utilization
Memory utilization
Redis latency
Redis hit ratio
Database latency
Database connections
Kafka throughput
Kafka consumer lag
Queue depth
```

---

# 19. Load Testing

The system will be tested using a load-testing tool such as k6.

Testing will progressively increase traffic:

```text
1K RPS
   |
   v
10K RPS
   |
   v
50K RPS
   |
   v
100K RPS
   |
   v
500K+ RPS
```

The actual maximum throughput will depend on the infrastructure used.

For each test, the following will be recorded:

```text
Requests/sec
P50 latency
P95 latency
P99 latency
Error rate
CPU
Memory
Database utilization
Redis utilization
Kafka consumer lag
```

---

# 20. Performance Optimization Cycle

Every major architectural change will follow:

```text
Build
  |
  v
Benchmark
  |
  v
Identify Bottleneck
  |
  v
Optimize
  |
  v
Benchmark Again
  |
  v
Compare Results
```

Example:

```text
Initial System

20K RPS
P99 = 800ms

        |
        v

Introduce Redis

70K RPS
P99 = 250ms

        |
        v

Horizontal Scaling

150K RPS
P99 = 130ms
```

The actual values will be determined through experiments.

---

# 21. Containerization

All application components will eventually run as Docker containers.

Example:

```text
Docker

├── API Gateway
├── User Service
├── Product Service
├── Order Service
├── Notification Worker
├── Analytics Worker
├── Redis
├── Kafka
└── PostgreSQL
```

---

# 22. Kubernetes Architecture

The containerized application will eventually be deployed using Kubernetes.

```text
                     Kubernetes Cluster
                            |
          +-----------------+-----------------+
          |                 |                 |
          v                 v                 v
      Gateway Pods      Service Pods      Worker Pods
          |                 |                 |
          +-----------------+-----------------+
                            |
                 +----------+----------+
                 |                     |
                 v                     v
               Redis                 Kafka
                 |                     |
                 +----------+----------+
                            |
                            v
                        PostgreSQL
```

Kubernetes will provide:

- Service discovery
- Container orchestration
- Health checks
- Rolling deployments
- Horizontal scaling
- Self-healing

---

# 23. Autoscaling

The system should automatically increase application instances when traffic or resource utilization increases.

Example:

```text
Low Traffic

3 Gateway Pods
```

Traffic increases:

```text
          Traffic Spike
               |
               v
        Kubernetes HPA
               |
               v
3 Pods → 10 Pods → 20 Pods
```

When traffic decreases, instances can scale down.

---

# 24. Multi-Region Architecture

Multi-region deployment will be considered after the single-region architecture is stable.

Target architecture:

```text
                         Global Users
                              |
                              v
                       Global Routing
                         /        \
                        /          \
                       v            v
                  Region A       Region B
                     |              |
                Load Balancer  Load Balancer
                     |              |
                  Gateway        Gateway
                     |              |
                  Services       Services
                     |              |
                  Redis          Redis
                     |              |
                  Database      Database
```

Areas to investigate:

- Geographic routing
- Data locality
- Replication
- Failover
- Disaster recovery
- Consistency
- RPO
- RTO

---

# 25. Cloud Architecture

The final system may be deployed on AWS.

Potential mapping:

| System Component | AWS Service |
|---|---|
| CDN | CloudFront |
| DNS | Route 53 |
| Load Balancer | ALB / NLB |
| Kubernetes | EKS |
| Database | RDS |
| Redis | ElastiCache |
| Kafka | MSK |
| Object Storage | S3 |
| Monitoring | CloudWatch |

Cloud deployment will be introduced only after the local architecture is functional.

---

# 26. Security Considerations

The platform will progressively implement:

- HTTPS/TLS
- JWT authentication
- Authorization
- API rate limiting
- Input validation
- Secure secret management
- Password hashing
- Database credential protection
- Container security
- Network isolation
- Least-privilege IAM

Sensitive credentials should never be committed to GitHub.

---

# 27. Data Flow Summary

### Read Path

```text
Client
  ↓
CDN
  ↓
Load Balancer
  ↓
API Gateway
  ↓
Rate Limiter
  ↓
Redis
  ↓
Backend Service
  ↓
Database
```

### Write Path

```text
Client
  ↓
Load Balancer
  ↓
API Gateway
  ↓
Backend Service
  ↓
Database
  ↓
Kafka
  ↓
Consumers
```

---

# 28. Technology Stack

## Programming Language

```text
Java 21
```

## Backend

```text
Spring Boot
Spring Security
Maven
```

## Database

```text
PostgreSQL
```

## Caching

```text
Redis
```

## Messaging

```text
Apache Kafka
```

## Containerization

```text
Docker
```

## Orchestration

```text
Kubernetes
```

## Monitoring

```text
Prometheus
Grafana
OpenTelemetry
```

## Load Testing

```text
k6
```

## Cloud

```text
AWS
```

---

# 29. Project Evolution

The architecture will evolve through the following stages:

```text
Stage 1
Simple Spring Boot Application
        |
        v
Stage 2
API Gateway
        |
        v
Stage 3
Load Balancing
        |
        v
Stage 4
Redis Caching
        |
        v
Stage 5
Distributed Rate Limiting
        |
        v
Stage 6
Kafka Event Processing
        |
        v
Stage 7
Database Optimization
        |
        v
Stage 8
Fault Tolerance
        |
        v
Stage 9
Docker
        |
        v
Stage 10
Kubernetes
        |
        v
Stage 11
Autoscaling
        |
        v
Stage 12
Observability
        |
        v
Stage 13
Load Testing
        |
        v
Stage 14
Multi-Region Architecture
        |
        v
Stage 15
AWS Deployment
```

---

# 30. Key Engineering Questions

Throughout development, the following questions will be investigated:

### Scalability

- How can the Gateway handle increasing traffic?
- When should horizontal scaling be introduced?
- What becomes the bottleneck first?

### Caching

- When should data be cached?
- How should cache invalidation work?
- What happens during a cache stampede?
- How should hot keys be handled?

### Rate Limiting

- Which algorithm should be used?
- How can rate limits remain consistent across Gateway instances?
- What happens if Redis becomes unavailable?

### Kafka

- How many partitions are required?
- How should events be ordered?
- What happens when a consumer fails?
- How should retries be handled?

### Database

- Which queries require indexes?
- When are read replicas necessary?
- When would partitioning help?
- When would sharding become necessary?

### Reliability

- What happens when a service fails?
- How do we prevent cascading failures?
- Where should retries be used?
- Which operations need idempotency?

### Performance

- What is our current throughput?
- What is our P99 latency?
- What is the current bottleneck?
- Did an optimization actually improve performance?

---

# 31. Final Architecture Goal

The final target architecture is:

```text
                              INTERNET
                                 |
                                 v
                              CDN
                                 |
                                 v
                         GLOBAL ROUTING
                                 |
                                 v
                          LOAD BALANCER
                                 |
                +----------------+----------------+
                |                |                |
                v                v                v
              Gateway          Gateway          Gateway
                |                |                |
                +----------------+----------------+
                                 |
                    +------------+------------+
                    |                         |
                    v                         v
               Redis Cluster            Auth Service
                    |
                    v
              Backend Services
             /        |        \
            v         v         v
         User      Product     Order
        Service    Service    Service
                                  |
                                  v
                               Kafka
                                  |
                 +----------------+----------------+
                 |                |                |
                 v                v                v
              Payment        Notification      Analytics
              Consumer          Worker           Consumer
                 |
                 v
             PostgreSQL
                 |
            +----+----+
            |         |
            v         v
         Primary   Replicas

       ┌─────────────────────────────┐
       │        OBSERVABILITY        │
       │                             │
       │ Prometheus                  │
       │ Grafana                     │
       │ OpenTelemetry               │
       │ Logs                        │
       │ Distributed Tracing         │
       └─────────────────────────────┘
```

---

# 32. Success Criteria

The project will be considered successful when:

- The platform can process API requests through a scalable Gateway layer.
- Gateway instances can be horizontally scaled.
- Distributed rate limiting works correctly.
- Frequently accessed data can be served from Redis.
- Events can be processed asynchronously using Kafka.
- Backend services can tolerate individual component failures.
- Database performance can be measured and optimized.
- Services can be containerized.
- Kubernetes can manage service instances.
- Autoscaling can respond to increased load.
- System metrics and traces are visible.
- Load tests demonstrate measurable throughput and latency.
- Performance bottlenecks and optimizations are documented.
- The architecture can be extended toward multi-region deployment.
- The final implementation is documented well enough to explain all major design decisions in an SDE interview.

---

# 33. Development Philosophy

This project will prioritize **understanding over technology accumulation**.

A new component will only be introduced when there is a clear engineering reason for it.

For every component, the project should answer:

> What problem does this solve?

> Why is this solution appropriate?

> What are its limitations?

> What alternatives were considered?

> How does it affect scalability, latency, reliability, and cost?

The system will be developed incrementally and benchmarked throughout its evolution.

---

# 34. Current Status

```text
[✓] Problem Definition
[✓] Initial HLD
[ ] Capacity Estimation
[ ] API Specification
[ ] Database Schema
[ ] Project Initialization
[ ] Basic Backend
[ ] API Gateway
[ ] Load Balancing
[ ] Redis
[ ] Rate Limiting
[ ] Kafka
[ ] Database Scaling
[ ] Fault Tolerance
[ ] Docker
[ ] Kubernetes
[ ] Autoscaling
[ ] Observability
[ ] Load Testing
[ ] Multi-Region
[ ] AWS Deployment
```

**Current milestone:** HLD completed.

**Next milestone:** Detailed capacity estimation, API design, database schema, and Phase 1 implementation plan.
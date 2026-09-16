# Capacity Estimation

## Distributed High-Scale API Gateway & Event Processing Platform

**Version:** 1.0  
**Status:** Initial Estimation  
**Date:** September 2026

---

# 1. Purpose

Capacity estimation helps determine the approximate resources and infrastructure required to support the expected workload.

Before implementing the system, we estimate:

- Number of users
- Daily active users
- Requests per day
- Average requests per second
- Peak requests per second
- Read/write traffic
- Network bandwidth
- Database operations
- Cache requirements
- Kafka throughput
- Storage requirements
- Initial service instance requirements

These numbers are estimates used for architectural planning.

The actual capacity of the implementation will be determined later through load testing.

---

# 2. Important Assumptions

The initial system is designed around the following hypothetical workload.

| Parameter | Assumption |
|---|---:|
| Total registered users | 100 million |
| Daily Active Users (DAU) | 10 million |
| Requests per active user/day | 100 |
| Peak traffic multiplier | 10x average |
| Read requests | 90% |
| Write requests | 10% |
| Average request size | 2 KB |
| Average response size | 5 KB |
| Requests generating Kafka events | 10% |
| Average event size | 1 KB |
| Cache hit ratio target | 90% |

These assumptions represent a high-scale workload for architectural planning and are not claims about actual production traffic.

---

# 3. Daily Request Volume

Daily active users:

```text
10 million users
```

Requests per active user per day:

```text
100 requests
```

Therefore:

```text
Daily Requests

= 10,000,000 × 100

= 1,000,000,000 requests/day
```

Therefore:

> **Estimated daily request volume = 1 billion requests/day**

---

# 4. Average Requests Per Second

There are:

```text
24 × 60 × 60

= 86,400 seconds/day
```

Therefore:

```text
Average RPS

= 1,000,000,000 / 86,400

≈ 11,574 requests/sec
```

Therefore:

> **Average traffic ≈ 11.6K requests/sec**

---

# 5. Peak Requests Per Second

Real-world traffic is not uniformly distributed throughout the day.

For initial architectural planning, we assume a peak multiplier of:

```text
10x
```

Therefore:

```text
Peak RPS

= 11,574 × 10

≈ 115,740 requests/sec
```

Rounded:

> **Peak traffic ≈ 116K requests/sec**

This becomes the initial high-level capacity target for the architecture.

```text
Average Traffic
      |
      v
   ~11.6K RPS
      |
      | 10x peak factor
      v
   ~116K RPS
```

---

# 6. Read vs Write Traffic

We assume:

```text
90% Read
10% Write
```

At peak traffic:

```text
Total = 116K RPS
```

### Read traffic

```text
116,000 × 0.90

= 104,400 requests/sec
```

Approximately:

> **104K read requests/sec**

### Write traffic

```text
116,000 × 0.10

= 11,600 requests/sec
```

Approximately:

> **11.6K write requests/sec**

Therefore:

```text
                    116K RPS
                       |
             +---------+---------+
             |                   |
             v                   v
          READS               WRITES
        ~104K RPS            ~11.6K RPS
```

This read-heavy workload motivates the use of Redis caching and database read replicas.

---

# 7. Network Bandwidth

## 7.1 Incoming Traffic

Average request size:

```text
2 KB
```

Peak request rate:

```text
116,000 requests/sec
```

Therefore:

```text
116,000 × 2 KB

= 232,000 KB/sec
```

Approximately:

```text
≈ 232 MB/sec
```

In bits:

```text
232 MB/sec × 8

≈ 1.86 Gbps
```

Therefore:

> **Estimated incoming application traffic ≈ 1.86 Gbps**

---

# 8. Outgoing Traffic

Average response size:

```text
5 KB
```

Peak requests:

```text
116,000 requests/sec
```

Therefore:

```text
116,000 × 5 KB

= 580,000 KB/sec
```

Approximately:

```text
≈ 580 MB/sec
```

In bits:

```text
580 × 8

≈ 4.64 Gbps
```

Therefore:

> **Estimated outgoing application traffic ≈ 4.64 Gbps**

---

# 9. Total Network Traffic

Approximate application-level traffic:

```text
Incoming ≈ 232 MB/sec

Outgoing ≈ 580 MB/sec
```

Therefore:

```text
Total ≈ 812 MB/sec
```

or approximately:

```text
≈ 6.5 Gbps
```

This is an application-level estimate and does not include protocol overhead, TLS overhead, retries, replication traffic, logging traffic, or internal service-to-service traffic.

---

# 10. Database Load Without Caching

Peak read traffic:

```text
≈ 104K reads/sec
```

If every read goes directly to PostgreSQL:

```text
104K read operations/sec
```

This can become a major database bottleneck.

Therefore, the architecture introduces Redis caching.

```text
             104K read requests/sec
                       |
                       v
                     Redis
                  /        \
                 /          \
             Cache Hit    Cache Miss
                |              |
                v              v
             Response       Database
```

---

# 11. Database Load With 90% Cache Hit Ratio

Assumed cache hit ratio:

```text
90%
```

Therefore:

```text
Cache Miss Ratio = 10%
```

Database reads:

```text
104,400 × 0.10

= 10,440 reads/sec
```

Therefore:

> **Estimated database read load ≈ 10.4K reads/sec**

This represents a major reduction from:

```text
104K reads/sec
```

to approximately:

```text
10.4K reads/sec
```

The actual cache hit ratio will be measured later.

---

# 12. Database Write Load

Peak write traffic:

```text
≈ 11,600 writes/sec
```

Therefore the database must initially be designed around approximately:

```text
~11.6K writes/sec
```

plus internal database operations such as:

- Index updates
- Transactions
- Constraint checks
- Replication
- Background maintenance

This is one of the areas we will benchmark and optimize during implementation.

---

# 13. Redis Request Load

Redis will handle:

- Cache reads
- Cache writes
- Rate limiting
- Idempotency
- Other shared state

For a simple cache-aside read:

```text
Request
   |
   v
Redis GET
   |
   +---- HIT ---> Response
   |
   +---- MISS --> Database
                    |
                    v
                 Redis SET
```

With approximately:

```text
104K read requests/sec
```

Redis could receive approximately:

```text
~104K GET operations/sec
```

plus:

```text
~10K SET operations/sec
```

for cache misses, depending on the exact caching implementation.

Rate limiting will add additional Redis operations.

Therefore, Redis capacity will be benchmarked separately.

---

# 14. Kafka Throughput

Assumption:

```text
10% of requests generate events
```

Peak request rate:

```text
116,000 requests/sec
```

Therefore:

```text
116,000 × 10%

= 11,600 events/sec
```

Assuming an average event size of:

```text
1 KB
```

Kafka ingress:

```text
11,600 × 1 KB

≈ 11.6 MB/sec
```

Therefore:

> **Initial estimated Kafka ingress ≈ 11.6 MB/sec**

This does not include replication traffic.

---

# 15. Kafka Consumers

Potential consumers:

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

Each consumer group may have multiple consumer instances.

For example:

```text
Payment Consumer Group

Consumer 1
Consumer 2
Consumer 3
Consumer 4
```

The number of consumers and partitions will be determined through throughput and consumer-lag measurements.

---

# 16. Storage Estimation

Suppose the platform generates approximately:

```text
1 billion requests/day
```

If we retain approximately:

```text
1 KB
```

of request/event metadata per request:

```text
1,000,000,000 × 1 KB

≈ 1 TB/day
```

Therefore:

```text
1 day    ≈ 1 TB
7 days   ≈ 7 TB
30 days  ≈ 30 TB
```

This demonstrates why raw request logs should not necessarily be stored indefinitely in the transactional PostgreSQL database.

A more appropriate architecture is:

```text
Application
     |
     +----> PostgreSQL
     |       Transactional Data
     |
     +----> Kafka
             |
             v
       Analytics / Storage
```

Long-term analytical data can eventually be stored in object storage or a dedicated analytics system.

---

# 17. Redis Memory Estimation

Suppose we eventually cache:

```text
10 million hot objects
```

and each cached object requires approximately:

```text
2 KB
```

of serialized data.

Raw data:

```text
10,000,000 × 2 KB

≈ 20 GB
```

Additional memory is required for:

- Redis object overhead
- Keys
- Metadata
- Expiration information
- Replication
- Fragmentation

Therefore, actual Redis memory requirements will be higher than the raw 20 GB estimate.

A production deployment would provision sufficient headroom rather than operating at maximum memory utilization.

---

# 18. API Gateway Capacity

Peak incoming traffic:

```text
≈ 116K requests/sec
```

The Gateway layer should therefore scale horizontally.

Instead of:

```text
1 Gateway
```

we use:

```text
                  Load Balancer
                  /     |     \
                 v      v      v
               GW-1   GW-2   GW-3
```

If one Gateway instance can experimentally handle:

```text
10K RPS
```

under the defined workload and latency target, then the theoretical minimum would be:

```text
116K / 10K

≈ 12 instances
```

However, production-style deployment should include additional capacity for:

- Traffic spikes
- Instance failures
- Rolling deployments
- Resource headroom

Therefore, the actual number of instances will be determined through load testing.

---

# 19. Initial Service Capacity Model

The architecture will initially treat the following components as horizontally scalable:

```text
API Gateway
User Service
Product Service
Order Service
Kafka Consumers
```

Example:

```text
                 Load Balancer
                       |
          +------------+------------+
          |            |            |
          v            v            v
       Gateway      Gateway      Gateway
          |            |            |
          +------------+------------+
                       |
                 Backend Services
                       |
          +------------+------------+
          |            |            |
          v            v            v
        User       Product       Order
       Service      Service      Service
```

The exact number of instances will be determined experimentally.

---

# 20. Availability Considerations

No critical component should depend on a single application instance.

Instead:

```text
Gateway 1
Gateway 2
Gateway 3
```

If:

```text
Gateway 2 ❌
```

the load balancer should route traffic to:

```text
Gateway 1
Gateway 3
```

Similarly, stateful infrastructure such as Redis, Kafka, and PostgreSQL will eventually use highly available configurations.

---

# 21. Capacity Summary

| Metric | Estimated Value |
|---|---:|
| Total users | 100M |
| Daily active users | 10M |
| Requests/user/day | 100 |
| Requests/day | 1B |
| Average RPS | ~11.6K |
| Peak multiplier | 10x |
| Peak RPS | ~116K |
| Peak read RPS | ~104K |
| Peak write RPS | ~11.6K |
| Avg request size | 2 KB |
| Avg response size | 5 KB |
| Incoming bandwidth | ~232 MB/s |
| Outgoing bandwidth | ~580 MB/s |
| Total application traffic | ~812 MB/s |
| Cache hit target | 90% |
| Estimated DB read load with cache | ~10.4K/s |
| Estimated Kafka events | ~11.6K/s |
| Kafka ingress | ~11.6 MB/s |
| Estimated raw metadata storage | ~1 TB/day |
| Example cache dataset | ~20 GB raw |

---

# 22. Important Bottlenecks

Based on the initial estimates, potential bottlenecks include:

### 1. Database

Approximately:

```text
~10.4K reads/sec
~11.6K writes/sec
```

after caching assumptions.

Potential solutions:

```text
Indexes
Query optimization
Connection pooling
Read replicas
Partitioning
Sharding
```

---

### 2. API Gateway

Potentially:

```text
~116K requests/sec
```

must be distributed across multiple Gateway instances.

Potential solutions:

```text
Horizontal scaling
Load balancing
Stateless architecture
Connection pooling
Efficient request processing
```

---

### 3. Redis

Redis may receive:

```text
~104K+ cache operations/sec
```

plus rate-limiting operations.

Potential solutions:

```text
Redis replication
Redis Cluster
Key distribution
Avoiding hot keys
Connection pooling
```

---

### 4. Kafka

Initial event rate:

```text
~11.6K events/sec
```

Potential solutions:

```text
Multiple partitions
Multiple consumers
Consumer groups
Batch processing
Monitoring consumer lag
```

---

### 5. Network

Estimated application-level bandwidth:

```text
~6.5 Gbps
```

Potential solutions:

```text
CDN
Compression
Connection reuse
Multiple network interfaces
Horizontal scaling
```

---

# 23. Scaling Strategy

The system will not immediately deploy a massive distributed architecture.

It will evolve through measurable stages.

```text
Stage 1
Single Application
        |
        v
Stage 2
Multiple Application Instances
        |
        v
Stage 3
Load Balancer
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
Kafka
        |
        v
Stage 7
Database Read Replicas
        |
        v
Stage 8
Database Partitioning / Sharding
        |
        v
Stage 9
Kubernetes
        |
        v
Stage 10
Autoscaling
        |
        v
Stage 11
Multi-Region
```

---

# 24. Theoretical Capacity vs Actual Capacity

The calculations in this document are **capacity-planning assumptions**.

They do not mean that the current implementation can process:

```text
116K RPS
```

or:

```text
1 billion requests/day
```

The actual capacity will depend on:

- CPU
- Memory
- Network
- Database configuration
- Redis configuration
- Kafka configuration
- Number of instances
- Request complexity
- Payload size
- Cache hit ratio
- Cloud infrastructure

Therefore, later phases will perform actual load tests.

The final project will report measured results such as:

```text
Requests/sec
P50 latency
P95 latency
P99 latency
Error rate
CPU utilization
Memory utilization
Database utilization
Redis utilization
Kafka consumer lag
```

---

# 25. Benchmarking Strategy

Each major architectural improvement will be benchmarked.

Example:

```text
                 BASELINE
                    |
                    v
              Load Testing
                    |
                    v
             Identify Bottleneck
                    |
                    v
               Add Redis
                    |
                    v
              Load Testing
                    |
                    v
             Compare Results
```

Example benchmark format:

| Version | Architecture | RPS | P95 | P99 | Error Rate |
|---|---|---:|---:|---:|---:|
| V1 | Basic Backend | TBD | TBD | TBD | TBD |
| V2 | + Load Balancer | TBD | TBD | TBD | TBD |
| V3 | + Redis | TBD | TBD | TBD | TBD |
| V4 | + Rate Limiting | TBD | TBD | TBD | TBD |
| V5 | + Kafka | TBD | TBD | TBD | TBD |
| V6 | + Kubernetes | TBD | TBD | TBD | TBD |

`TBD` values will be replaced with actual measurements during implementation.

---

# 26. Engineering Conclusions

The initial capacity estimation suggests that the system should be designed around:

```text
~11.6K average RPS
~116K peak RPS
~104K peak read RPS
~11.6K peak write RPS
```

The architecture therefore requires:

```text
Stateless API Gateways
        +
Load Balancing
        +
Redis Caching
        +
Distributed Rate Limiting
        +
Kafka
        +
Database Optimization
        +
Horizontal Scaling
        +
Observability
```

The most important principle is:

> **Capacity estimates guide the architecture, while benchmarks validate the architecture.**

The project will continuously compare estimated behavior with measured behavior and update the architecture accordingly.

---

# 27. Next Step

The next design activity is:

**API Specification + Data Model**

We will define:

```text
Users
Products
Orders
Events
```

and specify:

```text
HTTP methods
Endpoints
Request schemas
Response schemas
Error responses
Authentication
Idempotency
Database tables
Indexes
Relationships
```

Only after these are defined will implementation begin.
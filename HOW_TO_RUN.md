# How to Run — gRPC Microservices Example

## Prerequisites

| Tool | Version |
|---|---|
| Java | 17+ |
| Maven | 3.8+ |

---

## Project Structure

```
grpc/
├── pom.xml                        ← Parent Maven POM
├── grpc-proto/                    ← Shared .proto definitions (auto-generates Java stubs)
│   └── src/main/proto/
│       ├── user.proto
│       └── product.proto
├── user-service/                  ← gRPC SERVER  (HTTP :8080, gRPC :9090)
│   └── src/main/java/...
└── product-service/               ← gRPC CLIENT + REST API (HTTP :8081)
    └── src/main/java/...
```

### Communication Flow

```
HTTP Client
    │
    │  POST /api/products  (REST)
    ▼
product-service (:8081)
    │
    │  getUser(userId)  (gRPC over HTTP/2)
    ▼
user-service (:9090)
```

---

## Step 1 — Build the Entire Project

```bash
cd grpc
mvn clean install -DskipTests
```

This compiles the `.proto` files in `grpc-proto` and generates Java stubs that both services use.

---

## Step 2 — Start user-service (gRPC Server)

Open a terminal and run:

```bash
cd user-service
mvn spring-boot:run
```

**What starts:**
- Spring Boot HTTP on `http://localhost:8080`
- gRPC server on port `9090`
- H2 in-memory DB with 4 pre-seeded users
- H2 Console at `http://localhost:8080/h2-console`

---

## Step 3 — Start product-service (gRPC Client + REST)

Open a **second terminal** and run:

```bash
cd product-service
mvn spring-boot:run
```

**What starts:**
- REST API on `http://localhost:8081`
- gRPC client configured to call `user-service` at `localhost:9090`
- H2 in-memory DB for products

---

## Step 4 — Test the gRPC Communication

### 4.1 Get the seeded user IDs from user-service

```bash
curl http://localhost:8080/api/users
```

**Response:**
```json
[
  { "id": "abc-123-...", "name": "Jatin Ghataliya", "email": "jatin@example.com", "age": 30, "status": "ACTIVE" },
  { "id": "def-456-...", "name": "Alice Smith",     "email": "alice@example.com", "age": 25, "status": "ACTIVE" },
  ...
]
```

Copy one of the `id` values (e.g. `abc-123-...`) for the next step.

---

### 4.2 Create a Product (triggers gRPC call to user-service)

```bash
curl -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "MacBook Pro",
    "description": "16-inch M3 Pro laptop",
    "price": 2499.99,
    "ownerId": "<paste-user-id-here>"
  }'
```

**What happens internally:**
1. `product-service` receives the REST request
2. It calls `user-service` via **gRPC** (`getUser(userId)`)
3. `user-service` responds with user details over HTTP/2 + Protobuf
4. `product-service` saves the product and returns an enriched response

**Response:**
```json
{
  "productId": "xyz-789-...",
  "name": "MacBook Pro",
  "description": "16-inch M3 Pro laptop",
  "price": 2499.99,
  "ownerId": "abc-123-...",
  "ownerName": "Jatin Ghataliya",
  "ownerEmail": "jatin@example.com"
}
```

> **ownerName** and **ownerEmail** are fetched live from `user-service` via **gRPC** — not stored in `product-service`!

---

### 4.3 List All Products (enriched with owner info via gRPC)

```bash
curl http://localhost:8081/api/products
```

---

### 4.4 Get a Single Product

```bash
curl http://localhost:8081/api/products/<product-id>
```

---

### 4.5 List Products by Owner

```bash
curl http://localhost:8081/api/products/owner/<user-id>
```

---

### 4.6 Create a User via user-service REST

You can also create new users:

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Dave Brown", "email": "dave@example.com", "age": 32}'
```

*(Add a POST endpoint to UserController if needed, or use the gRPC path directly)*

---

## Ports Summary

| Service | HTTP Port | gRPC Port | H2 Console |
|---|---|---|---|
| user-service | 8080 | 9090 | http://localhost:8080/h2-console |
| product-service | 8081 | — (client only) | http://localhost:8081/h2-console |

---

## H2 Console Access

- **URL:** `http://localhost:8080/h2-console` (user-service)
- **JDBC URL:** `jdbc:h2:mem:userdb`
- **Username:** `sa`
- **Password:** *(leave blank)*

---

## Troubleshooting

| Issue | Fix |
|---|---|
| `UNAVAILABLE: io exception` | Ensure `user-service` is running before starting `product-service` |
| `DEADLINE_EXCEEDED` | user-service is slow to start — wait a few seconds and retry |
| `NOT_FOUND` on product create | The `ownerId` doesn't exist in user-service — use `/api/users` to get valid IDs |
| Build fails on proto compilation | Run `mvn clean install` from the root to compile protos first |

# gRPC Microservices — Java Spring Boot

> A hands-on **gRPC** implementation with two Spring Boot microservices communicating over **HTTP/2 + Protobuf**.

---

## Architecture

```
HTTP Client
    │
    │  REST API (:8081)
    ▼
┌─────────────────────┐          gRPC (HTTP/2 + Protobuf)        ┌──────────────────────┐
│   product-service   │ ──────────────────────────────────────►  │    user-service      │
│   (gRPC Client)     │  getUser(userId) / listUsers()           │    (gRPC Server)     │
│   REST  :8081       │ ◄──────────────────────────────────────  │    gRPC  :9090       │
│   H2 DB (products)  │        UserResponse (Protobuf)           │    HTTP  :8080       │
└─────────────────────┘                                          │    H2 DB (users)     │
                                                                 └──────────────────────┘
```

**Key pattern:** `product-service` stores only the `owner_id`. When a product is fetched, it calls `user-service` via **gRPC** to enrich the response with `ownerName` and `ownerEmail` in real-time.

---

## Modules

| Module | Role | Ports |
|---|---|---|
| `grpc-proto` | Shared `.proto` IDL — generates Java stubs for both services | — |
| `user-service` | gRPC Server — manages users | HTTP :8080, gRPC :9090 |
| `product-service` | gRPC Client + REST API — manages products, calls user-service | HTTP :8081 |

---

## gRPC Communication Patterns Used

| Pattern | Where | Description |
|---|---|---|
| **Unary RPC** | `getUser()`, `createUser()` | Single request → single response |
| **Server Streaming** | `listUsers()` | Single request → stream of responses |

---

## Quick Start

```bash
# 1. Build everything (compiles .proto files → Java stubs)
mvn clean install -DskipTests

# 2. Start user-service (Terminal 1)
cd user-service && mvn spring-boot:run

# 3. Start product-service (Terminal 2)
cd product-service && mvn spring-boot:run

# 4. Get user IDs
curl http://localhost:8080/api/users

# 5. Create a product (triggers gRPC call to user-service)
curl -X POST http://localhost:8081/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"MacBook Pro","description":"Laptop","price":2499.99,"ownerId":"<user-id>"}'
```

See **[HOW_TO_RUN.md](./HOW_TO_RUN.md)** for the full step-by-step guide with expected responses.

---

## Proto Definitions

### [`user.proto`](grpc-proto/src/main/proto/user.proto)
```protobuf
service UserService {
  rpc GetUser    (GetUserRequest)    returns (UserResponse);          // Unary
  rpc ListUsers  (ListUsersRequest)  returns (stream UserResponse);   // Server Streaming
  rpc CreateUser (CreateUserRequest) returns (UserResponse);          // Unary
}
```

### [`product.proto`](grpc-proto/src/main/proto/product.proto)
```protobuf
service ProductService {
  rpc GetProduct    (GetProductRequest)    returns (ProductResponse);
  rpc ListProducts  (ListProductsRequest)  returns (stream ProductResponse);
  rpc CreateProduct (CreateProductRequest) returns (ProductResponse);
}
```

---

## REST API Endpoints (product-service :8081)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/products` | Create product (validates owner via gRPC) |
| `GET` | `/api/products` | List all products (enriched with owner info) |
| `GET` | `/api/products/{id}` | Get product by ID |
| `GET` | `/api/products/owner/{ownerId}` | Products by owner |

## REST API Endpoints (user-service :8080)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/users` | List all users |
| `GET` | `/api/users/{id}` | Get user by ID |

---

## Tech Stack

| Technology | Purpose |
|---|---|
| Java 17 | Language |
| Spring Boot 3.2 | Application framework |
| gRPC 1.60 | RPC communication framework |
| Protocol Buffers 3 | Binary serialization (IDL) |
| grpc-spring-boot-starter | Spring integration for gRPC |
| H2 Database | In-memory DB (dev/demo) |
| Lombok | Boilerplate reduction |

---

## Documentation

- 📘 [RPC_DOCUMENTATION.md](./RPC_DOCUMENTATION.md) — Complete RPC & gRPC technical reference
- 🚀 [HOW_TO_RUN.md](./HOW_TO_RUN.md) — Step-by-step run guide with cURL examples

---

## Author

**Jatin Ghataliya**  
GitHub: [@Jatinghataliya](https://github.com/Jatinghataliya)  
Email: prajapati.jatin94@gmail.com

# Remote Procedure Call (RPC) — Complete Technical Documentation

> **Version:** 1.0.0  
> **Author:** Jatin Ghataliya  
> **Last Updated:** 2026  

---

## Table of Contents

1. [Introduction to RPC](#1-introduction-to-rpc)
2. [How RPC Works Internally](#2-how-rpc-works-internally)
3. [Core Components](#3-core-components)
4. [Serialization & IDL](#4-serialization--idl)
5. [RPC Frameworks & Variants](#5-rpc-frameworks--variants)
6. [gRPC — The Modern Standard](#6-grpc--the-modern-standard)
7. [gRPC Communication Patterns](#7-grpc-communication-patterns)
8. [Protobuf (Protocol Buffers)](#8-protobuf-protocol-buffers)
9. [gRPC with Java (Spring Boot)](#9-grpc-with-java-spring-boot)
10. [Error Handling in gRPC](#10-error-handling-in-grpc)
11. [Failure Handling Patterns](#11-failure-handling-patterns)
12. [Security in gRPC](#12-security-in-grpc)
13. [Service Discovery & Load Balancing](#13-service-discovery--load-balancing)
14. [Observability & Distributed Tracing](#14-observability--distributed-tracing)
15. [RPC vs REST vs Messaging](#15-rpc-vs-rest-vs-messaging)
16. [When to Use RPC](#16-when-to-use-rpc)
17. [Best Practices](#17-best-practices)
18. [Glossary](#18-glossary)

---

## 1. Introduction to RPC

**Remote Procedure Call (RPC)** is a communication protocol that allows a program running on one machine to invoke a function/procedure on a **remote machine** — as if it were a local function call. The calling program does not need to understand the underlying network details.

### Key Idea

```
Without RPC:
  Client sends HTTP request → parses JSON → maps to object → calls business logic

With RPC:
  Client calls orderService.createOrder(request)  ← looks like a local method call!
  (All the network, serialization, and transport is hidden by the framework)
```

### Why RPC in Microservices?

In a microservices architecture, dozens (or hundreds) of services must communicate. RPC provides:

| Benefit | Description |
|---|---|
| **Performance** | Binary serialization (Protobuf) is 5–10x faster than JSON |
| **Strong Contracts** | IDL enforces a strict API contract between services |
| **Code Generation** | Client/server stubs are auto-generated — no boilerplate |
| **Streaming** | Native support for bidirectional streaming |
| **Polyglot** | Works across Java, Go, Python, Node.js, C++, etc. |

---

## 2. How RPC Works Internally

```
┌─────────────────────────────────────────────────────────────────────┐
│                          RPC Flow                                    │
│                                                                     │
│  Service A (Client)              Service B (Server)                 │
│  ─────────────────               ────────────────────               │
│                                                                     │
│  1. Client calls              7. Server Skeleton                    │
│     local stub method            deserializes params                │
│         │                              │                            │
│  2. Stub serializes           6. Network delivers                   │
│     (marshals) params            the request                        │
│         │                              │                            │
│  3. Stub sends over           5. Server Skeleton                    │
│     network (HTTP/2)             receives request                   │
│         │                              │                            │
│  4. ─────────────── Network ──────────────────────►                 │
│         │                              │                            │
│  8. ◄────────────── Network ──────────────────────                  │
│         │                              │                            │
│  9. Stub deserializes         8. Server executes                    │
│     the response                 actual function                    │
│         │                              │                            │
│ 10. Returns result to         9. Skeleton serializes                │
│     the calling code             and sends response                 │
└─────────────────────────────────────────────────────────────────────┘
```

### Step-by-Step Breakdown

| Step | Actor | Action |
|---|---|---|
| 1 | Client Code | Calls a local method on the generated **Client Stub** |
| 2 | Client Stub | **Marshals** (serializes) parameters into binary/JSON |
| 3 | Client Stub | Opens a network connection and sends the payload |
| 4 | Network | Transports the data to the server |
| 5 | Server Skeleton | Receives the incoming request |
| 6 | Server Skeleton | **Unmarshals** (deserializes) parameters |
| 7 | Server Skeleton | Invokes the actual server function |
| 8 | Server Function | Executes business logic, returns result |
| 9 | Server Skeleton | Serializes the result and sends it back |
| 10 | Client Stub | Receives, deserializes, and returns the result to caller |

---

## 3. Core Components

### 3.1 Client Stub (Proxy)
- Auto-generated code that mimics the server's interface
- Handles: serialization, connection management, and request dispatching
- The client code calls this as if calling a local function

### 3.2 Server Skeleton
- Auto-generated server-side glue code
- Receives incoming requests, deserializes them
- Delegates to the actual server implementation

### 3.3 RPC Runtime
- Manages the underlying transport (TCP, HTTP/2)
- Handles connection pooling, multiplexing, and flow control

### 3.4 IDL (Interface Definition Language)
- A language-agnostic file defining the service contract
- Used to generate both client stub and server skeleton in any language

```
         ┌──────────────┐
         │  .proto file │  ← IDL (the contract)
         │  (IDL)       │
         └──────┬───────┘
                │  protoc compiler
        ┌───────┴────────┐
        │                │
 ┌──────▼──────┐   ┌──────▼──────┐
 │ Client Stub │   │Server Skel. │
 │ (Java/Go/..)│   │ (Java/Go/..)│
 └─────────────┘   └─────────────┘
```

---

## 4. Serialization & IDL

### 4.1 Serialization Formats Comparison

| Format | Type | Size | Speed | Human Readable | Usage |
|---|---|---|---|---|---|
| **JSON** | Text | Large | Slow | ✅ Yes | REST APIs |
| **XML** | Text | Very Large | Very Slow | ✅ Yes | SOAP, Legacy |
| **Protobuf** | Binary | Small | Very Fast | ❌ No | gRPC |
| **Thrift** | Binary | Small | Fast | ❌ No | Apache Thrift |
| **Avro** | Binary | Small | Fast | ❌ No | Kafka Schema |
| **MessagePack** | Binary | Medium | Fast | ❌ No | IoT, Embedded |

### 4.2 Protobuf vs JSON — Size Comparison

```
Same data serialized:

JSON:    {"user_id": "u123", "name": "Jatin", "age": 30}
         ─────────────────────────────────────────────────
         Size: ~46 bytes

Protobuf: [binary encoded]
          ──────────────────
          Size: ~12 bytes  (≈ 74% smaller!)
```

---

## 5. RPC Frameworks & Variants

### 5.1 Framework Overview

```
                          RPC Ecosystem
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
       Modern              Classic              Lightweight
          │                    │                    │
    ┌─────┴─────┐        ┌─────┴─────┐       ┌─────┴─────┐
    │   gRPC    │        │   SOAP    │       │ JSON-RPC  │
    │  (Google) │        │(WS-*Std.) │       │           │
    └─────┬─────┘        └─────┬─────┘       └─────┬─────┘
          │                    │                    │
    ┌─────┴─────┐        ┌─────┴─────┐       ┌─────┴─────┐
    │  Thrift   │        │  XML-RPC  │       │   Twirp   │
    │(Facebook) │        │ (Legacy)  │       │  (Twitch) │
    └───────────┘        └───────────┘       └───────────┘
```

### 5.2 Framework Comparison

| Framework | Transport | Serialization | Streaming | Language Support | Best For |
|---|---|---|---|---|---|
| **gRPC** | HTTP/2 | Protobuf | ✅ Full | 10+ languages | Microservices |
| **Apache Thrift** | TCP/HTTP | Binary/JSON | ✅ Partial | 20+ languages | Multi-language systems |
| **JSON-RPC** | HTTP/WebSocket | JSON | ❌ No | All languages | Simple APIs |
| **XML-RPC** | HTTP | XML | ❌ No | All languages | Legacy systems |
| **SOAP** | HTTP/SMTP | XML + WSDL | ❌ No | All languages | Enterprise/Banking |
| **Twirp** | HTTP 1.1/2 | Protobuf/JSON | ❌ No | Go, others | Simple gRPC alternative |

---

## 6. gRPC — The Modern Standard

**gRPC** (Google Remote Procedure Call) is an open-source, high-performance RPC framework developed by Google. It is the industry standard for microservice-to-microservice communication.

### 6.1 Architecture

```
┌─────────────────────────────────────────────────────┐
│                    gRPC Architecture                 │
│                                                     │
│  ┌─────────────┐         ┌─────────────────────┐   │
│  │  .proto IDL │────────►│    protoc Compiler   │   │
│  └─────────────┘         └──────────┬──────────┘   │
│                                     │               │
│                         ┌───────────┴───────────┐   │
│                         │                       │   │
│               ┌─────────▼──────┐   ┌────────────▼─┐│
│               │  Client Stub   │   │Server Interface││
│               │  (Generated)   │   │  (Generated)  ││
│               └────────┬───────┘   └──────┬────────┘│
│                        │                  │         │
│               ┌────────▼───────┐   ┌──────▼────────┐│
│               │  gRPC Client   │   │  gRPC Server  ││
│               │  (Your Code)   │   │  (Your Code)  ││
│               └────────┬───────┘   └──────┬────────┘│
│                        │   HTTP/2 +        │         │
│                        └───Protobuf────────┘         │
└─────────────────────────────────────────────────────┘
```

### 6.2 Why HTTP/2?

gRPC uses **HTTP/2** as its transport protocol, giving it major advantages over HTTP/1.1:

| Feature | HTTP/1.1 | HTTP/2 |
|---|---|---|
| **Multiplexing** | ❌ One request per connection | ✅ Multiple streams per connection |
| **Header Compression** | ❌ Repeated large headers | ✅ HPACK compression |
| **Server Push** | ❌ Not supported | ✅ Supported |
| **Streaming** | ❌ Limited (chunked) | ✅ Native bidirectional |
| **Binary Protocol** | ❌ Text-based | ✅ Binary framing |
| **Latency** | Higher | Lower |

### 6.3 gRPC Channel & Connection

```
Client                                           Server
  │                                                │
  │──── TCP Handshake (TLS) ───────────────────────│
  │                                                │
  │──── HTTP/2 Connection Established ─────────────│
  │                                                │
  │   Stream 1: CreateOrder() ──────────────────►  │
  │   Stream 3: GetUser()     ──────────────────►  │  ← Multiplexed!
  │   Stream 5: UpdateCart()  ──────────────────►  │
  │                                                │
  │  ◄──────────── Stream 1 Response ──────────── │
  │  ◄──────────── Stream 3 Response ──────────── │
  │  ◄──────────── Stream 5 Response ──────────── │
```

---

## 7. gRPC Communication Patterns

gRPC supports **4 communication patterns** — a major advantage over REST.

### 7.1 Unary RPC (Request-Response)

The simplest pattern: one request, one response.

```
Client ──── Request ────► Server
Client ◄─── Response ─── Server
```

**Use Cases:** CRUD operations, authentication, simple queries

```protobuf
// Proto Definition
service UserService {
  rpc GetUser (GetUserRequest) returns (GetUserResponse);
}
```

```java
// Java Client Usage
GetUserResponse response = userServiceStub.getUser(
    GetUserRequest.newBuilder().setUserId("u123").build()
);
```

---

### 7.2 Server Streaming RPC

Client sends one request; server sends back a **stream of responses**.

```
Client ──── Request ────────────────────────────────► Server
Client ◄─── Response 1 ─── Response 2 ─── Response 3  Server
```

**Use Cases:** Real-time price feeds, live logs, large dataset pagination

```protobuf
service StockService {
  rpc WatchPrice (StockRequest) returns (stream PriceUpdate);
}
```

```java
// Java Client Usage
stockServiceStub.watchPrice(
    StockRequest.newBuilder().setSymbol("AAPL").build(),
    new StreamObserver<PriceUpdate>() {
        @Override
        public void onNext(PriceUpdate update) {
            System.out.println("Price: " + update.getPrice());
        }

        @Override
        public void onCompleted() {
            System.out.println("Stream completed");
        }

        @Override
        public void onError(Throwable t) {
            System.err.println("Error: " + t.getMessage());
        }
    }
);
```

---

### 7.3 Client Streaming RPC

Client sends a **stream of requests**; server responds with a single response.

```
Client ──── Chunk 1 ─── Chunk 2 ─── Chunk 3 ────────► Server
Client ◄──────────────────────────────── Response ─── Server
```

**Use Cases:** File uploads, batch inserts, telemetry data ingestion

```protobuf
service FileService {
  rpc UploadFile (stream FileChunk) returns (UploadResponse);
}
```

```java
// Java Client Usage
StreamObserver<FileChunk> requestObserver = fileServiceStub.uploadFile(
    new StreamObserver<UploadResponse>() {
        @Override
        public void onNext(UploadResponse response) {
            System.out.println("Upload status: " + response.getStatus());
        }
        @Override public void onCompleted() {}
        @Override public void onError(Throwable t) {}
    }
);

// Send chunks
for (byte[] chunk : fileChunks) {
    requestObserver.onNext(FileChunk.newBuilder()
        .setData(ByteString.copyFrom(chunk))
        .build());
}
requestObserver.onCompleted();
```

---

### 7.4 Bidirectional Streaming RPC

Both client and server send **streams** independently and simultaneously.

```
Client ──── Msg 1 ──────────────── Msg 2 ──────► Server
Client ◄─── Reply 1 ─── Reply 2 ─────────────── Server
```

**Use Cases:** Chat applications, collaborative editing, live dashboards, gaming

```protobuf
service ChatService {
  rpc Chat (stream ChatMessage) returns (stream ChatMessage);
}
```

```java
// Java Client Usage
StreamObserver<ChatMessage> requestObserver = chatServiceStub.chat(
    new StreamObserver<ChatMessage>() {
        @Override
        public void onNext(ChatMessage message) {
            System.out.println("Received: " + message.getText());
        }
        @Override public void onCompleted() {}
        @Override public void onError(Throwable t) {}
    }
);

// Send and receive simultaneously
requestObserver.onNext(ChatMessage.newBuilder().setText("Hello!").build());
```

### 7.5 Patterns Summary

| Pattern | Proto Syntax | Client Sends | Server Sends | Use Case |
|---|---|---|---|---|
| **Unary** | `rpc Method(Req) returns (Res)` | 1 message | 1 message | CRUD, Auth |
| **Server Stream** | `rpc Method(Req) returns (stream Res)` | 1 message | N messages | Live feeds |
| **Client Stream** | `rpc Method(stream Req) returns (Res)` | N messages | 1 message | File upload |
| **Bidirectional** | `rpc Method(stream Req) returns (stream Res)` | N messages | N messages | Chat, Gaming |

---

## 8. Protobuf (Protocol Buffers)

Protocol Buffers (Protobuf) is Google's language-neutral, platform-neutral, extensible mechanism for **serializing structured data**.

### 8.1 Proto File Structure

```protobuf
syntax = "proto3";                          // Version declaration

package com.example.order;                  // Package namespace

import "google/protobuf/timestamp.proto";   // Import other protos

option java_multiple_files = true;
option java_package = "com.example.order";

// ── Service Definition ────────────────────────────────────────
service OrderService {
  rpc CreateOrder  (CreateOrderRequest)  returns (CreateOrderResponse);
  rpc GetOrder     (GetOrderRequest)     returns (OrderResponse);
  rpc ListOrders   (ListOrdersRequest)   returns (stream OrderResponse);
  rpc UpdateOrders (stream UpdateRequest) returns (UpdateSummary);
}

// ── Message Definitions ───────────────────────────────────────
message CreateOrderRequest {
  string  user_id   = 1;      // Field number 1
  string  product_id = 2;     // Field number 2
  int32   quantity  = 3;      // Field number 3
  double  price     = 4;      // Field number 4
}

message CreateOrderResponse {
  string order_id  = 1;
  string status    = 2;
  google.protobuf.Timestamp created_at = 3;
}

message OrderResponse {
  string order_id   = 1;
  string user_id    = 2;
  string product_id = 3;
  int32  quantity   = 4;
  double price      = 5;
  OrderStatus status = 6;     // Enum field
  repeated Item items = 7;    // List field
}

// ── Enum Definition ───────────────────────────────────────────
enum OrderStatus {
  ORDER_STATUS_UNSPECIFIED = 0;   // Always start at 0 in proto3
  PENDING   = 1;
  CONFIRMED = 2;
  SHIPPED   = 3;
  DELIVERED = 4;
  CANCELLED = 5;
}

// ── Nested Message ────────────────────────────────────────────
message Item {
  string name     = 1;
  int32  quantity = 2;
  double price    = 3;
}
```

### 8.2 Protobuf Scalar Types

| Proto Type | Java Type | Description |
|---|---|---|
| `double` | double | 64-bit float |
| `float` | float | 32-bit float |
| `int32` | int | 32-bit integer |
| `int64` | long | 64-bit integer |
| `uint32` | int | Unsigned 32-bit |
| `uint64` | long | Unsigned 64-bit |
| `bool` | boolean | Boolean |
| `string` | String | UTF-8 string |
| `bytes` | ByteString | Arbitrary bytes |

### 8.3 Field Rules

```protobuf
message Example {
  string   name     = 1;           // Singular (default in proto3)
  repeated string tags = 2;        // List / Array
  map<string, string> metadata = 3; // Map / Dictionary
  oneof payment {                  // One of these fields
    string credit_card = 4;
    string paypal_id   = 5;
  }
}
```

### 8.4 Wire Encoding

Protobuf uses a compact binary format. Each field is encoded as:

```
[Field Number + Wire Type] [Value]
      └── 3 bits              └── Variable length (varint, fixed, etc.)
```

This makes it far smaller than JSON — no field names are sent on the wire; only field numbers are used.

---

## 9. gRPC with Java (Spring Boot)

### 9.1 Maven Dependencies

```xml
<dependencies>
    <!-- gRPC Spring Boot Starter -->
    <dependency>
        <groupId>net.devh</groupId>
        <artifactId>grpc-spring-boot-starter</artifactId>
        <version>2.15.0.RELEASE</version>
    </dependency>

    <!-- Protobuf Java Runtime -->
    <dependency>
        <groupId>com.google.protobuf</groupId>
        <artifactId>protobuf-java</artifactId>
        <version>3.25.1</version>
    </dependency>

    <!-- gRPC Core -->
    <dependency>
        <groupId>io.grpc</groupId>
        <artifactId>grpc-netty-shaded</artifactId>
        <version>1.60.0</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- Protobuf Compiler Plugin -->
        <plugin>
            <groupId>org.xolstice.maven.plugins</groupId>
            <artifactId>protobuf-maven-plugin</artifactId>
            <version>0.6.1</version>
            <configuration>
                <protocArtifact>com.google.protobuf:protoc:3.25.1:exe:${os.detected.classifier}</protocArtifact>
                <pluginId>grpc-java</pluginId>
                <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.60.0:exe:${os.detected.classifier}</pluginArtifact>
            </configuration>
            <executions>
                <execution>
                    <goals>
                        <goal>compile</goal>
                        <goal>compile-custom</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 9.2 Server Implementation

```java
// OrderServiceImpl.java
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class OrderServiceImpl extends OrderServiceGrpc.OrderServiceImplBase {

    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // ── Unary RPC ─────────────────────────────────────────────
    @Override
    public void createOrder(CreateOrderRequest request,
                            StreamObserver<CreateOrderResponse> responseObserver) {
        try {
            Order order = orderRepository.save(
                new Order(request.getUserId(), request.getProductId(), request.getQuantity())
            );

            CreateOrderResponse response = CreateOrderResponse.newBuilder()
                .setOrderId(order.getId())
                .setStatus("CREATED")
                .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(
                Status.INTERNAL
                    .withDescription("Failed to create order: " + e.getMessage())
                    .asRuntimeException()
            );
        }
    }

    // ── Server Streaming RPC ──────────────────────────────────
    @Override
    public void listOrders(ListOrdersRequest request,
                           StreamObserver<OrderResponse> responseObserver) {
        orderRepository.findByUserId(request.getUserId())
            .forEach(order -> {
                responseObserver.onNext(toProto(order));
            });
        responseObserver.onCompleted();
    }
}
```

### 9.3 Client Implementation

```java
// OrderServiceClient.java
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceClient {

    @GrpcClient("order-service")
    private OrderServiceGrpc.OrderServiceBlockingStub orderStub;

    @GrpcClient("order-service")
    private OrderServiceGrpc.OrderServiceStub asyncOrderStub;

    // ── Blocking (Synchronous) Call ───────────────────────────
    public CreateOrderResponse createOrder(String userId, String productId, int qty) {
        CreateOrderRequest request = CreateOrderRequest.newBuilder()
            .setUserId(userId)
            .setProductId(productId)
            .setQuantity(qty)
            .build();

        return orderStub.withDeadlineAfter(5, TimeUnit.SECONDS)
                        .createOrder(request);
    }

    // ── Async Server Streaming Call ───────────────────────────
    public void streamOrders(String userId) {
        asyncOrderStub.listOrders(
            ListOrdersRequest.newBuilder().setUserId(userId).build(),
            new StreamObserver<OrderResponse>() {
                @Override
                public void onNext(OrderResponse order) {
                    System.out.println("Received order: " + order.getOrderId());
                }
                @Override
                public void onError(Throwable t) {
                    System.err.println("Stream error: " + t.getMessage());
                }
                @Override
                public void onCompleted() {
                    System.out.println("All orders received");
                }
            }
        );
    }
}
```

### 9.4 Application Configuration

```yaml
# application.yml

# gRPC Server Config
grpc:
  server:
    port: 9090
    security:
      enabled: false   # Set true for TLS in production

# gRPC Client Config
grpc:
  client:
    order-service:
      address: static://localhost:9090
      negotiationType: plaintext   # Use TLS in production

spring:
  application:
    name: order-service
```

---

## 10. Error Handling in gRPC

gRPC has a rich set of **status codes** (different from HTTP status codes).

### 10.1 gRPC Status Codes

| Code | Number | Meaning | HTTP Equivalent |
|---|---|---|---|
| `OK` | 0 | Success | 200 |
| `CANCELLED` | 1 | Request cancelled by client | 499 |
| `UNKNOWN` | 2 | Unknown error | 500 |
| `INVALID_ARGUMENT` | 3 | Bad request parameters | 400 |
| `DEADLINE_EXCEEDED` | 4 | Timeout exceeded | 504 |
| `NOT_FOUND` | 5 | Resource not found | 404 |
| `ALREADY_EXISTS` | 6 | Resource already exists | 409 |
| `PERMISSION_DENIED` | 7 | Not authorized | 403 |
| `RESOURCE_EXHAUSTED` | 8 | Rate limit exceeded | 429 |
| `FAILED_PRECONDITION` | 9 | System not in required state | 400 |
| `ABORTED` | 10 | Concurrency conflict | 409 |
| `UNAUTHENTICATED` | 16 | No valid credentials | 401 |
| `INTERNAL` | 13 | Server-side error | 500 |
| `UNAVAILABLE` | 14 | Service temporarily down | 503 |

### 10.2 Throwing Errors on Server

```java
// In your server implementation
@Override
public void getOrder(GetOrderRequest request,
                     StreamObserver<OrderResponse> responseObserver) {

    if (request.getOrderId().isEmpty()) {
        responseObserver.onError(
            Status.INVALID_ARGUMENT
                .withDescription("order_id cannot be empty")
                .asRuntimeException()
        );
        return;
    }

    Order order = orderRepository.findById(request.getOrderId())
        .orElseThrow(() ->
            Status.NOT_FOUND
                .withDescription("Order not found: " + request.getOrderId())
                .asRuntimeException()
        );

    responseObserver.onNext(toProto(order));
    responseObserver.onCompleted();
}
```

### 10.3 Handling Errors on Client

```java
try {
    OrderResponse order = orderStub.getOrder(request);
} catch (StatusRuntimeException e) {
    switch (e.getStatus().getCode()) {
        case NOT_FOUND:
            System.out.println("Order not found");
            break;
        case DEADLINE_EXCEEDED:
            System.out.println("Request timed out — retry?");
            break;
        case UNAVAILABLE:
            System.out.println("Service down — circuit breaker triggered");
            break;
        default:
            System.out.println("Unexpected error: " + e.getStatus());
    }
}
```

---

## 11. Failure Handling Patterns

Network calls can fail. Always design for failure with these patterns:

### 11.1 Timeout / Deadline

Always set a deadline on RPC calls to prevent hanging forever.

```java
// Set a 3-second deadline
OrderResponse response = orderStub
    .withDeadlineAfter(3, TimeUnit.SECONDS)
    .getOrder(request);
```

### 11.2 Retry with Exponential Backoff

```java
// Retry config in application.yml (grpc-spring-boot-starter)
grpc:
  client:
    order-service:
      retry:
        max-attempts: 3
        initial-backoff: 100ms
        max-backoff: 1s
        backoff-multiplier: 2.0
        retryable-status-codes: UNAVAILABLE, DEADLINE_EXCEEDED
```

```java
// Manual retry example
int maxRetries = 3;
long backoffMs = 100;

for (int attempt = 0; attempt < maxRetries; attempt++) {
    try {
        return orderStub.getOrder(request);
    } catch (StatusRuntimeException e) {
        if (e.getStatus().getCode() == Status.Code.UNAVAILABLE && attempt < maxRetries - 1) {
            Thread.sleep(backoffMs);
            backoffMs *= 2;   // Exponential backoff
        } else {
            throw e;
        }
    }
}
```

### 11.3 Circuit Breaker

Prevents cascading failures by temporarily stopping calls to a failing service.

```
States:
  CLOSED ──(too many failures)──► OPEN ──(timeout)──► HALF-OPEN
    ▲                                                       │
    └───────────────(success)──────────────────────────────┘

  CLOSED    : Normal operation, calls go through
  OPEN      : Calls fail immediately (no network call made)
  HALF-OPEN : Test call goes through to check if service recovered
```

```java
// Using Resilience4j Circuit Breaker
@Bean
public CircuitBreaker orderServiceCircuitBreaker(CircuitBreakerRegistry registry) {
    return registry.circuitBreaker("order-service", CircuitBreakerConfig.custom()
        .failureRateThreshold(50)           // Open if 50% of calls fail
        .waitDurationInOpenState(Duration.ofSeconds(30))
        .slidingWindowSize(10)
        .build()
    );
}

// Apply to gRPC call
CircuitBreaker.decorateCheckedSupplier(circuitBreaker, () ->
    orderStub.getOrder(request)
).get();
```

### 11.4 Bulkhead

Isolates failures so one service's problems don't consume all resources:

```java
// Limit concurrent gRPC calls to order-service
ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("order-service",
    ThreadPoolBulkheadConfig.custom()
        .maxThreadPoolSize(10)
        .coreThreadPoolSize(5)
        .queueCapacity(20)
        .build()
);
```

---

## 12. Security in gRPC

### 12.1 TLS (Transport Layer Security)

Always use TLS in production to encrypt gRPC traffic.

```yaml
# Server TLS config
grpc:
  server:
    security:
      enabled: true
      certificate-chain: classpath:certs/server.crt
      private-key: classpath:certs/server.key
```

```yaml
# Client TLS config
grpc:
  client:
    order-service:
      negotiationType: TLS
      security:
        trust-cert-collection: classpath:certs/ca.crt
```

### 12.2 Mutual TLS (mTLS)

Both client AND server authenticate each other — ideal for service-to-service auth.

```
Without mTLS:  Client verifies Server identity only
With mTLS:     Client ↔ Server both verify each other
```

### 12.3 Authentication with Interceptors

```java
// Server-side auth interceptor
public class AuthInterceptor implements ServerInterceptor {

    @Override
    public <Req, Resp> ServerCall.Listener<Req> interceptCall(
            ServerCall<Req, Resp> call,
            Metadata headers,
            ServerCallHandler<Req, Resp> next) {

        String token = headers.get(
            Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER)
        );

        if (!isValidToken(token)) {
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid token"), new Metadata());
            return new ServerCall.Listener<>() {};
        }

        return next.startCall(call, headers);
    }
}
```

```java
// Client-side token injection
Metadata headers = new Metadata();
headers.put(
    Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER),
    "Bearer " + jwtToken
);

OrderServiceGrpc.OrderServiceBlockingStub stub =
    orderStub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers));
```

---

## 13. Service Discovery & Load Balancing

### 13.1 The Problem

RPC clients need to know **where** the server is. In dynamic environments (Kubernetes, Docker Swarm), service IPs change constantly.

### 13.2 Service Discovery Approaches

```
Client-Side Discovery                   Server-Side Discovery
─────────────────────                   ──────────────────────
  ┌────────┐                             ┌────────┐
  │ Client │──query──► Service Registry  │ Client │──────────────┐
  └────┬───┘ ◄──IPs──                    └────────┘              │
       │                                                   Load Balancer
       ├──────────────► Instance 1        ┌────────┐ ◄───────────┤
       └──────────────► Instance 2        │Instance│             │
                                          └────────┘      Service Registry
                                                                  │
  Tools: Eureka, Consul                  Instance 1 ─────────────┘
                                         Instance 2

  Tools: Kubernetes Services, AWS ALB
```

### 13.3 Load Balancing Strategies

| Strategy | Description | Best For |
|---|---|---|
| **Round Robin** | Requests distributed evenly | Uniform services |
| **Least Connection** | Route to server with fewest active calls | Variable load |
| **Pick First** | Always use first available server | Simple setups |
| **Weighted Round Robin** | Servers get proportional traffic | Different capacities |

```java
// gRPC client-side load balancing (Round Robin)
ManagedChannel channel = ManagedChannelBuilder
    .forTarget("consul://order-service/order-service")
    .defaultLoadBalancingPolicy("round_robin")
    .build();
```

---

## 14. Observability & Distributed Tracing

### 14.1 The Challenge

```
Request comes in → Service A → Service B → Service C → Service D
                                              ↑
                                          Error here!
```

Without tracing, finding where the error occurred is nearly impossible.

### 14.2 Distributed Tracing with OpenTelemetry

```java
// Add OpenTelemetry gRPC interceptor
OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
    .setTracerProvider(...)
    .build();

ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
    .intercept(GrpcTelemetry.create(openTelemetry).newClientInterceptor())
    .build();
```

### 14.3 Key Metrics to Monitor

| Metric | Description |
|---|---|
| `grpc_server_started_total` | Total RPC calls received |
| `grpc_server_handled_total` | Total RPCs completed (with status code) |
| `grpc_server_handling_seconds` | Latency histogram |
| `grpc_client_started_total` | Total RPCs initiated by client |
| `grpc_client_handled_total` | Total client RPCs completed |

```yaml
# Enable gRPC metrics with Micrometer (Spring Boot)
management:
  metrics:
    export:
      prometheus:
        enabled: true
  endpoints:
    web:
      exposure:
        include: prometheus, health, info
```

---

## 15. RPC vs REST vs Messaging

### 15.1 Decision Matrix

| Criteria | gRPC / RPC | REST | Messaging (Kafka/RabbitMQ) |
|---|---|---|---|
| **Communication** | Synchronous | Synchronous | Asynchronous |
| **Coupling** | Tight | Moderate | Loose |
| **Performance** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Contract** | Strict (IDL) | Optional (OpenAPI) | Schema Registry |
| **Streaming** | ✅ Native | Limited (SSE) | ✅ Native |
| **Browser Support** | ⚠️ gRPC-Web only | ✅ Full | ⚠️ Via WebSocket |
| **Human Readable** | ❌ Binary | ✅ JSON | Depends on format |
| **Error Handling** | Rich status codes | HTTP status codes | Dead Letter Queues |
| **Retry** | Client-managed | Client-managed | Broker-managed |
| **Discoverability** | IDL required | Self-documenting (HAL) | Topic schema |

### 15.2 Architectural Positioning

```
                     Communication Patterns
                            │
             Synchronous    │    Asynchronous
             ───────────────┼─────────────────
                            │
    High       gRPC ●       │        ● Kafka
  Performance              │
                            │
                  REST ●   │   ● RabbitMQ
                            │
    Low                     │
  Performance  SOAP ●       │
                            │
               Internal    │    External / Event
```

---

## 16. When to Use RPC

### Use gRPC / RPC When:

- ✅ **Internal microservice-to-microservice** communication
- ✅ **Low latency** is critical (real-time systems, trading platforms)
- ✅ **Strict schema** enforcement is required between teams
- ✅ **Streaming** data is needed (bidirectional or server push)
- ✅ **Polyglot** services (Java talks to Go talks to Python)
- ✅ **High throughput** systems (binary serialization saves bandwidth)

### Use REST When:

- ✅ **Public-facing APIs** consumed by external developers
- ✅ **Browser clients** (no gRPC-Web setup)
- ✅ **Simple CRUD** operations without performance constraints
- ✅ **Human-readable debugging** is important

### Use Messaging When:

- ✅ **Decoupled, async workflows** (order placed → notify inventory → notify shipping)
- ✅ **Fire-and-forget** operations
- ✅ **Event sourcing** and **CQRS** patterns
- ✅ **Fan-out** to multiple consumers
- ✅ **Backpressure** / rate-limiting is needed

---

## 17. Best Practices

### Proto Design
- 📌 Always use `proto3` syntax
- 📌 Never reuse field numbers — deprecated fields should be marked `reserved`
- 📌 Use `UNSPECIFIED = 0` as the first enum value
- 📌 Package names should match your team/service namespace
- 📌 Keep `.proto` files in a shared repository for cross-team contracts

### Performance
- 📌 Reuse `ManagedChannel` — don't create a new channel per request
- 📌 Use async stubs for high-throughput scenarios
- 📌 Enable `keepAlive` on channels to avoid reconnection overhead
- 📌 Use connection pooling for downstream services

### Reliability
- 📌 Always set **deadlines** on every RPC call
- 📌 Implement **retry with exponential backoff** for transient failures
- 📌 Use **circuit breakers** for downstream dependency failures
- 📌 Propagate **trace context** via gRPC metadata headers

### Security
- 📌 Always use **TLS** in production
- 📌 Use **mTLS** for sensitive service-to-service calls
- 📌 Validate authentication tokens in a **server interceptor**
- 📌 Never log raw request/response bodies in production

### Versioning
- 📌 Add new fields — don't rename or remove existing ones
- 📌 Use `reserved` to mark deleted field numbers
- 📌 Consider versioned packages (`v1`, `v2`) for breaking changes

```protobuf
message UserRequest {
  string user_id = 1;
  // string old_name = 2; ← DELETED
  reserved 2;             // ← Mark as reserved so no one reuses it
  reserved "old_name";    // ← Also reserve the name
}
```

---

## 18. Glossary

| Term | Definition |
|---|---|
| **RPC** | Remote Procedure Call — invoking a function on a remote machine |
| **gRPC** | Google's open-source RPC framework using HTTP/2 and Protobuf |
| **Protobuf** | Protocol Buffers — Google's binary serialization format |
| **IDL** | Interface Definition Language — language-agnostic contract file |
| **Stub** | Auto-generated client-side proxy for making RPC calls |
| **Skeleton** | Auto-generated server-side code that handles incoming RPC calls |
| **Marshalling** | Serializing data into a transmittable format |
| **Unmarshalling** | Deserializing received data back into objects |
| **Channel** | A connection to a gRPC server |
| **Deadline** | Maximum time allowed for an RPC call to complete |
| **Interceptor** | Middleware that runs before/after each RPC call |
| **Streaming** | Sending multiple messages over a single RPC call |
| **mTLS** | Mutual TLS — both client and server authenticate each other |
| **Circuit Breaker** | Pattern to stop calling a failing service temporarily |
| **Backoff** | Increasing wait time between retries |
| **Bulkhead** | Pattern to isolate failures using resource limits |
| **Service Mesh** | Infrastructure layer for service-to-service communication (e.g. Istio) |
| **Load Balancing** | Distributing traffic across multiple server instances |

---

*Documentation maintained by [Jatin Ghataliya](https://github.com/Jatinghataliya)*

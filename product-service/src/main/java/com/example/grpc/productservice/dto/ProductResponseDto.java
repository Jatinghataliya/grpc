package com.example.grpc.productservice.dto;

import lombok.Builder;
import lombok.Data;

/**
 * REST API response DTO — enriches Product with owner info fetched from user-service via gRPC.
 */
@Data
@Builder
public class ProductResponseDto {
    private String productId;
    private String name;
    private String description;
    private double price;
    private String ownerId;
    private String ownerName;    // ← from user-service via gRPC
    private String ownerEmail;   // ← from user-service via gRPC
}

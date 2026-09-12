package com.example.grpc.productservice.dto;

import lombok.Data;

/**
 * REST API request DTO for creating a product.
 */
@Data
public class CreateProductRequestDto {
    private String name;
    private String description;
    private double price;
    private String ownerId;
}

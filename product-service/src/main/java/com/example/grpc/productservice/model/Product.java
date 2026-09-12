package com.example.grpc.productservice.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private double price;

    /**
     * References the User who owns this product.
     * Resolved via gRPC call to user-service at query time.
     */
    @Column(nullable = false)
    private String ownerId;
}

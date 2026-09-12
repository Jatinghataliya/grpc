package com.example.grpc.productservice.service;

import com.example.grpc.productservice.client.UserServiceGrpcClient;
import com.example.grpc.productservice.dto.CreateProductRequestDto;
import com.example.grpc.productservice.dto.ProductResponseDto;
import com.example.grpc.productservice.model.Product;
import com.example.grpc.productservice.repository.ProductRepository;
import com.example.grpc.user.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final UserServiceGrpcClient userGrpcClient;

    /**
     * Creates a product — first validates the owner exists via gRPC call to user-service.
     */
    public ProductResponseDto createProduct(CreateProductRequestDto dto) {
        // ── Step 1: Validate owner exists via gRPC ─────────────────────────────
        UserResponse owner = userGrpcClient.getUser(dto.getOwnerId())
            .orElseThrow(() -> new NoSuchElementException(
                "Owner not found in user-service: " + dto.getOwnerId()
            ));

        // ── Step 2: Persist the product ────────────────────────────────────────
        Product saved = productRepository.save(
            Product.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .ownerId(dto.getOwnerId())
                .build()
        );

        log.info("Product '{}' created for owner '{}'", saved.getName(), owner.getName());

        // ── Step 3: Return enriched response ───────────────────────────────────
        return toDto(saved, owner);
    }

    /**
     * Gets a product by ID, enriches it with owner info via gRPC.
     */
    public ProductResponseDto getProduct(String productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));

        UserResponse owner = userGrpcClient.getUser(product.getOwnerId())
            .orElse(null);

        return toDto(product, owner);
    }

    /**
     * Lists all products, enriching each with owner info via gRPC.
     */
    public List<ProductResponseDto> listProducts() {
        return productRepository.findAll().stream()
            .map(product -> {
                UserResponse owner = userGrpcClient.getUser(product.getOwnerId()).orElse(null);
                return toDto(product, owner);
            })
            .collect(Collectors.toList());
    }

    /**
     * Lists all products belonging to a specific owner (fetched via gRPC first).
     */
    public List<ProductResponseDto> listProductsByOwner(String ownerId) {
        UserResponse owner = userGrpcClient.getUser(ownerId)
            .orElseThrow(() -> new NoSuchElementException("Owner not found: " + ownerId));

        return productRepository.findByOwnerId(ownerId).stream()
            .map(product -> toDto(product, owner))
            .collect(Collectors.toList());
    }

    // ─── Helper: Entity + gRPC UserResponse → DTO ─────────────────────────────
    private ProductResponseDto toDto(Product product, UserResponse owner) {
        return ProductResponseDto.builder()
            .productId(product.getId())
            .name(product.getName())
            .description(product.getDescription())
            .price(product.getPrice())
            .ownerId(product.getOwnerId())
            .ownerName(owner  != null ? owner.getName()  : "Unknown")
            .ownerEmail(owner != null ? owner.getEmail() : "Unknown")
            .build();
    }
}

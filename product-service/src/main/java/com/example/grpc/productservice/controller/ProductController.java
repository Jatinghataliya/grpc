package com.example.grpc.productservice.controller;

import com.example.grpc.productservice.dto.CreateProductRequestDto;
import com.example.grpc.productservice.dto.ProductResponseDto;
import com.example.grpc.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * REST API for the product-service.
 * Internally calls user-service via gRPC to enrich responses with owner info.
 */
@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * POST /api/products
     * Creates a product. Validates the owner exists via gRPC call to user-service.
     */
    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(@RequestBody CreateProductRequestDto dto) {
        log.info("REST POST /api/products — name: {}, ownerId: {}", dto.getName(), dto.getOwnerId());
        try {
            ProductResponseDto response = productService.createProduct(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (NoSuchElementException e) {
            log.warn("Create product failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * GET /api/products
     * Lists all products enriched with owner name and email (via gRPC).
     */
    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> listProducts() {
        log.info("REST GET /api/products");
        return ResponseEntity.ok(productService.listProducts());
    }

    /**
     * GET /api/products/{id}
     * Gets a single product enriched with owner info (via gRPC).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProduct(@PathVariable String id) {
        log.info("REST GET /api/products/{}", id);
        try {
            return ResponseEntity.ok(productService.getProduct(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/products/owner/{ownerId}
     * Lists all products for a given owner, fetching owner info via gRPC.
     */
    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<ProductResponseDto>> listByOwner(@PathVariable String ownerId) {
        log.info("REST GET /api/products/owner/{}", ownerId);
        try {
            return ResponseEntity.ok(productService.listProductsByOwner(ownerId));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

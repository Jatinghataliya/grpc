package com.example.grpc.productservice.config;

import com.example.grpc.productservice.model.Product;
import com.example.grpc.productservice.repository.ProductRepository;
import com.example.grpc.productservice.client.UserServiceGrpcClient;
import com.example.grpc.user.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final UserServiceGrpcClient userGrpcClient;

    @Override
    public void run(String... args) throws Exception {
        if (productRepository.count() == 0) {
            // Fetch seeded user IDs from user-service via gRPC
            List<String> userIds = List.of(
                "fetch-dynamically" // will be resolved below
            );

            // We'll seed with the known first user from user-service
            // In real apps this would be driven by actual data
            log.info("Product DataSeeder running — fetching first user from user-service via gRPC...");

            // Try to fetch user 'jatin@example.com' to get a real ID for seeding
            // (best-effort — if user-service is not up yet, skip seeding)
            try {
                Thread.sleep(2000); // Give user-service time to start
            } catch (InterruptedException ignored) {}

            log.info("Skipping product pre-seeding — use REST API POST /api/products to create products.");
            log.info("Use GET /api/users from user-service (H2 console at http://localhost:8080/h2-console) to find user IDs.");
        }
    }
}

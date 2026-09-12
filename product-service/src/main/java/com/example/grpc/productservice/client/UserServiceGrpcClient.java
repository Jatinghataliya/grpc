package com.example.grpc.productservice.client;

import com.example.grpc.user.GetUserRequest;
import com.example.grpc.user.UserResponse;
import com.example.grpc.user.UserServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * gRPC Client for communicating with user-service.
 * Uses the auto-generated blocking stub from the shared grpc-proto module.
 */
@Slf4j
@Component
public class UserServiceGrpcClient {

    /**
     * @GrpcClient("user-service") injects a managed channel configured in application.yml
     * under grpc.client.user-service.*
     */
    @GrpcClient("user-service")
    private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

    /**
     * Fetches a user by ID from user-service via gRPC (Unary RPC).
     *
     * @param userId the user ID to look up
     * @return Optional<UserResponse> — empty if user not found or service unavailable
     */
    public Optional<UserResponse> getUser(String userId) {
        log.info("Calling user-service via gRPC for userId: {}", userId);

        try {
            UserResponse response = userServiceStub
                .withDeadlineAfter(5, TimeUnit.SECONDS)   // Always set a deadline!
                .getUser(
                    GetUserRequest.newBuilder()
                        .setUserId(userId)
                        .build()
                );
            log.info("Received user from gRPC: {} ({})", response.getName(), response.getEmail());
            return Optional.of(response);

        } catch (StatusRuntimeException e) {
            switch (e.getStatus().getCode()) {
                case NOT_FOUND:
                    log.warn("User not found via gRPC: {}", userId);
                    break;
                case DEADLINE_EXCEEDED:
                    log.error("gRPC call to user-service timed out for userId: {}", userId);
                    break;
                case UNAVAILABLE:
                    log.error("user-service is unavailable via gRPC");
                    break;
                default:
                    log.error("gRPC error: {} — {}", e.getStatus().getCode(), e.getMessage());
            }
            return Optional.empty();
        }
    }
}

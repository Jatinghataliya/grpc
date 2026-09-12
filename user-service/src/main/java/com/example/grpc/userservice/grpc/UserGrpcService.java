package com.example.grpc.userservice.grpc;

import com.example.grpc.user.*;
import com.example.grpc.userservice.model.User;
import com.example.grpc.userservice.repository.UserRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserRepository userRepository;

    // ─── Unary RPC: Get a single user by ID ───────────────────────────────────
    @Override
    public void getUser(GetUserRequest request, StreamObserver<UserResponse> responseObserver) {
        log.info("gRPC getUser called for id: {}", request.getUserId());

        if (request.getUserId().isBlank()) {
            responseObserver.onError(
                Status.INVALID_ARGUMENT
                    .withDescription("user_id must not be empty")
                    .asRuntimeException()
            );
            return;
        }

        userRepository.findById(request.getUserId())
            .ifPresentOrElse(
                user -> {
                    responseObserver.onNext(toProto(user));
                    responseObserver.onCompleted();
                },
                () -> responseObserver.onError(
                    Status.NOT_FOUND
                        .withDescription("User not found: " + request.getUserId())
                        .asRuntimeException()
                )
            );
    }

    // ─── Server Streaming RPC: Stream all users ────────────────────────────────
    @Override
    public void listUsers(ListUsersRequest request, StreamObserver<UserResponse> responseObserver) {
        int page     = Math.max(request.getPage(), 0);
        int pageSize = request.getPageSize() > 0 ? request.getPageSize() : 10;

        log.info("gRPC listUsers called — page: {}, size: {}", page, pageSize);

        List<User> users = userRepository.findAll(PageRequest.of(page, pageSize)).getContent();

        users.forEach(user -> responseObserver.onNext(toProto(user)));
        responseObserver.onCompleted();
    }

    // ─── Unary RPC: Create a new user ─────────────────────────────────────────
    @Override
    public void createUser(CreateUserRequest request, StreamObserver<UserResponse> responseObserver) {
        log.info("gRPC createUser called for email: {}", request.getEmail());

        if (request.getName().isBlank() || request.getEmail().isBlank()) {
            responseObserver.onError(
                Status.INVALID_ARGUMENT
                    .withDescription("name and email are required")
                    .asRuntimeException()
            );
            return;
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            responseObserver.onError(
                Status.ALREADY_EXISTS
                    .withDescription("User with email already exists: " + request.getEmail())
                    .asRuntimeException()
            );
            return;
        }

        User saved = userRepository.save(
            User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .age(request.getAge())
                .build()
        );

        log.info("User created with id: {}", saved.getId());
        responseObserver.onNext(toProto(saved));
        responseObserver.onCompleted();
    }

    // ─── Helper: JPA Entity → Protobuf Message ─────────────────────────────────
    private UserResponse toProto(User user) {
        return UserResponse.newBuilder()
            .setUserId(user.getId())
            .setName(user.getName())
            .setEmail(user.getEmail())
            .setAge(user.getAge())
            .setStatus(user.getStatus())
            .build();
    }
}

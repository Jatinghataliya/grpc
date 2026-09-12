package com.example.grpc.userservice.config;

import com.example.grpc.userservice.model.User;
import com.example.grpc.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.save(User.builder().name("Jatin Ghataliya").email("jatin@example.com").age(30).build());
            userRepository.save(User.builder().name("Alice Smith").email("alice@example.com").age(25).build());
            userRepository.save(User.builder().name("Bob Johnson").email("bob@example.com").age(35).build());
            userRepository.save(User.builder().name("Carol White").email("carol@example.com").age(28).build());
            log.info("Seeded 4 users into H2 database.");
        }
    }
}

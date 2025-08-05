package com.miniSIEM.config;

import com.miniSIEM.model.User;
import com.miniSIEM.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) throws Exception {
        // Create default admin user if no users exist
        if (!userService.existsByUsername("admin")) {
            log.info("Creating default admin user...");

            User admin = User.builder()
                    .username("admin")
                    .email("admin@minisiem.com")
                    .password("admin123") // Will be encoded by UserService
                    .roles(List.of(User.Role.ADMIN))
                    .build();

            try {
                userService.createUser(admin);
                log.info("Default admin user created successfully!");
                log.info("Username: admin");
                log.info("Password: admin123");
                log.info("Email: admin@minisiem.com");
            } catch (Exception e) {
                log.error("Failed to create default admin user: {}", e.getMessage());
            }
        }

        // Create default analyst user if doesn't exist
        if (!userService.existsByUsername("analyst")) {
            log.info("Creating default analyst user...");

            User analyst = User.builder()
                    .username("analyst")
                    .email("analyst@minisiem.com")
                    .password("analyst123")
                    .roles(List.of(User.Role.ANALYST))
                    .build();

            try {
                userService.createUser(analyst);
                log.info("Default analyst user created successfully!");
                log.info("Username: analyst");
                log.info("Password: analyst123");
            } catch (Exception e) {
                log.error("Failed to create default analyst user: {}", e.getMessage());
            }
        }

        // Create default viewer user if doesn't exist
        if (!userService.existsByUsername("viewer")) {
            log.info("Creating default viewer user...");

            User viewer = User.builder()
                    .username("viewer")
                    .email("viewer@minisiem.com")
                    .password("viewer123")
                    .roles(List.of(User.Role.VIEWER))
                    .build();

            try {
                userService.createUser(viewer);
                log.info("Default viewer user created successfully!");
                log.info("Username: viewer");
                log.info("Password: viewer123");
            } catch (Exception e) {
                log.error("Failed to create default viewer user: {}", e.getMessage());
            }
        }
    }
}
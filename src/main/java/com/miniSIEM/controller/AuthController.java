package com.miniSIEM.controller;

import com.miniSIEM.dto.AuthRequest;
import com.miniSIEM.dto.AuthResponse;
import com.miniSIEM.dto.RegisterRequest;
import com.miniSIEM.model.User;
import com.miniSIEM.security.JwtUtil;
import com.miniSIEM.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest authRequest) {
        try {
            log.info("Login attempt for user: {}", authRequest.getUsername());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequest.getUsername(),
                            authRequest.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String jwtToken = jwtUtil.generateToken(userDetails.getUsername());


            // Update last login time
            userService.updateLastLogin(userDetails.getUsername());

            AuthResponse response = AuthResponse.builder()
                    .token(jwtToken)
                    .username(userDetails.getUsername())
                    .authorities(userDetails.getAuthorities().stream()
                            .map(auth -> auth.getAuthority())
                            .toList())
                    .expiresAt(Instant.now().plusSeconds(86400)) // 24 hours
                    .build();

            log.info("Login successful for user: {}", authRequest.getUsername());
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for user: {}", authRequest.getUsername());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid username or password");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            log.info("Registration attempt for user: {}", registerRequest.getUsername());

            User user = User.builder()
                    .username(registerRequest.getUsername())
                    .email(registerRequest.getEmail())
                    .password(registerRequest.getPassword())
                    .roles(registerRequest.getRoles())
                    .build();

            User createdUser = userService.createUser(user);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("username", createdUser.getUsername());
            response.put("email", createdUser.getEmail());
            response.put("roles", createdUser.getRoles());

            log.info("Registration successful for user: {}", registerRequest.getUsername());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Registration failed for user: {} - {}", registerRequest.getUsername(), e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String jwt = authHeader.substring(7);
            String username = jwtUtil.extractUsername(jwt);

            UserDetails userDetails = userService.loadUserByUsername(username);

            if (jwtUtil.validateToken(jwt, userDetails)) {
                String newToken = jwtUtil.generateToken(userDetails.getUsername());


                AuthResponse response = AuthResponse.builder()
                        .token(newToken)
                        .username(userDetails.getUsername())
                        .authorities(userDetails.getAuthorities().stream()
                                .map(auth -> auth.getAuthority())
                                .toList())
                        .expiresAt(Instant.now().plusSeconds(86400))
                        .build();

                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid token");
                return ResponseEntity.badRequest().body(error);
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Token refresh failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userService.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> response = new HashMap<>();
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("roles", user.getRoles());
            response.put("lastLoginAt", user.getLastLoginAt());
            response.put("createdAt", user.getCreatedAt());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
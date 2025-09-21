package com.haekitchenapp.recipeapp.service;

import com.haekitchenapp.recipeapp.entity.User;
import com.haekitchenapp.recipeapp.model.response.auth.JwtResponse;
import com.haekitchenapp.recipeapp.model.response.auth.LoginRequest;
import com.haekitchenapp.recipeapp.model.response.auth.RegisterRequest;
import com.haekitchenapp.recipeapp.repository.UserRepository;
import com.haekitchenapp.recipeapp.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        log.info("Starting authentication for user: {}", loginRequest.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
            log.info("Authentication successful for user: {}", loginRequest.getUsername());

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Security context set for user: {}", loginRequest.getUsername());

            String jwt = jwtUtils.generateJwtToken(authentication);
            log.debug("JWT token generated for user: {}", loginRequest.getUsername());

            User user = (User) authentication.getPrincipal();
            log.info("Login completed successfully for user: {} (ID: {})", user.getUsername(), user.getId());

            return new JwtResponse(jwt, user.getId(), user.getUsername(), user.getEmail());
        } catch (Exception e) {
            log.error("Authentication failed for user: {}. Error: {}", loginRequest.getUsername(), e.getMessage());
            throw e;
        }
    }

    public void registerUser(RegisterRequest registerRequest) {
        log.info("Starting user registration for username: {} and email: {}",
                   registerRequest.getUsername(), registerRequest.getEmail());

        // Check if username already exists
        boolean usernameExists = userRepository.existsByUsername(registerRequest.getUsername());
        log.debug("Username '{}' exists check: {}", registerRequest.getUsername(), usernameExists);

        if (usernameExists) {
            log.warn("Registration failed - Username already taken: {}", registerRequest.getUsername());
            throw new RuntimeException("Error: Username is already taken!");
        }

        // Check if email already exists
        boolean emailExists = userRepository.existsByEmail(registerRequest.getEmail());
        log.debug("Email '{}' exists check: {}", registerRequest.getEmail(), emailExists);

        if (emailExists) {
            log.warn("Registration failed - Email already in use: {}", registerRequest.getEmail());
            throw new RuntimeException("Error: Email is already in use!");
        }

        log.debug("Encoding password for user: {}", registerRequest.getUsername());
        String encodedPassword = encoder.encode(registerRequest.getPassword());
        log.debug("Password encoded successfully for user: {}", registerRequest.getUsername());

        User user = new User(
            registerRequest.getUsername(),
            registerRequest.getEmail(),
            encodedPassword
        );

        log.debug("Created User entity for: {}", registerRequest.getUsername());

        try {
            User savedUser = userRepository.save(user);
            log.info("User registered successfully: {} (ID: {})", savedUser.getUsername(), savedUser.getId());
        } catch (Exception e) {
            log.error("Failed to save user: {}. Error: {}", registerRequest.getUsername(), e.getMessage());
            throw new RuntimeException("Error: Failed to register user - " + e.getMessage());
        }
    }
}

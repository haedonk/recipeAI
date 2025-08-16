package com.haekitchenapp.recipeapp.service;

import com.haekitchenapp.recipeapp.entity.User;
import com.haekitchenapp.recipeapp.model.response.auth.JwtResponse;
import com.haekitchenapp.recipeapp.model.response.auth.LoginRequest;
import com.haekitchenapp.recipeapp.model.response.auth.RegisterRequest;
import com.haekitchenapp.recipeapp.repository.UserRepository;
import com.haekitchenapp.recipeapp.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        logger.info("Starting authentication for user: {}", loginRequest.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
            logger.info("Authentication successful for user: {}", loginRequest.getUsername());

            SecurityContextHolder.getContext().setAuthentication(authentication);
            logger.debug("Security context set for user: {}", loginRequest.getUsername());

            String jwt = jwtUtils.generateJwtToken(authentication);
            logger.debug("JWT token generated for user: {}", loginRequest.getUsername());

            User user = (User) authentication.getPrincipal();
            logger.info("Login completed successfully for user: {} (ID: {})", user.getUsername(), user.getId());

            return new JwtResponse(jwt, user.getId(), user.getUsername(), user.getEmail());
        } catch (Exception e) {
            logger.error("Authentication failed for user: {}. Error: {}", loginRequest.getUsername(), e.getMessage());
            throw e;
        }
    }

    public User registerUser(RegisterRequest registerRequest) {
        logger.info("Starting user registration for username: {} and email: {}",
                   registerRequest.getUsername(), registerRequest.getEmail());

        // Check if username already exists
        boolean usernameExists = userRepository.existsByUsername(registerRequest.getUsername());
        logger.debug("Username '{}' exists check: {}", registerRequest.getUsername(), usernameExists);

        if (usernameExists) {
            logger.warn("Registration failed - Username already taken: {}", registerRequest.getUsername());
            throw new RuntimeException("Error: Username is already taken!");
        }

        // Check if email already exists
        boolean emailExists = userRepository.existsByEmail(registerRequest.getEmail());
        logger.debug("Email '{}' exists check: {}", registerRequest.getEmail(), emailExists);

        if (emailExists) {
            logger.warn("Registration failed - Email already in use: {}", registerRequest.getEmail());
            throw new RuntimeException("Error: Email is already in use!");
        }

        logger.debug("Encoding password for user: {}", registerRequest.getUsername());
        String encodedPassword = passwordEncoder.encode(registerRequest.getPassword());
        logger.debug("Password encoded successfully for user: {}", registerRequest.getUsername());

        User user = new User(
            registerRequest.getUsername(),
            registerRequest.getEmail(),
            encodedPassword
        );

        logger.debug("Created User entity for: {}", registerRequest.getUsername());

        try {
            User savedUser = userRepository.save(user);
            logger.info("User registered successfully: {} (ID: {})", savedUser.getUsername(), savedUser.getId());
            return savedUser;
        } catch (Exception e) {
            logger.error("Failed to save user: {}. Error: {}", registerRequest.getUsername(), e.getMessage());
            throw new RuntimeException("Error: Failed to register user - " + e.getMessage());
        }
    }
}

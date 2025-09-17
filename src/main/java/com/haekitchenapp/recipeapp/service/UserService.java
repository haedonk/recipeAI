package com.haekitchenapp.recipeapp.service;

import com.haekitchenapp.recipeapp.entity.EmailVerification;
import com.haekitchenapp.recipeapp.entity.User;
import com.haekitchenapp.recipeapp.exception.InvalidValidationCodeException;
import com.haekitchenapp.recipeapp.exception.UserNotFoundException;
import com.haekitchenapp.recipeapp.model.request.email.VerifyEmailRequestDto;
import com.haekitchenapp.recipeapp.model.response.ApiResponse;
import com.haekitchenapp.recipeapp.repository.EmailVerificationRepository;
import com.haekitchenapp.recipeapp.repository.UserRepository;
import com.haekitchenapp.recipeapp.utility.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserMapper userMapper;

    /**
     * Retrieves a user ID by username with caching support.
     * @param username The username to look up
     * @return The user ID if found
     * @throws UserNotFoundException if no user exists with the given username
     */
    @Cacheable(value = "userIdByUsername", key = "#username", unless = "#result == null")
    public User getUserByUsername(String username) {
        log.info("Fetching user ID for username: {}", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found with username: {}", username);
                    return new UserNotFoundException("User not found with username: " + username);
                });
    }

    @Cacheable(value = "userById", key = "#id", unless = "#result == null")
    public User getUserById(Long id) {
        log.info("Fetching user by ID: {}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with ID: {}", id);
                    return new UserNotFoundException("User not found with ID: " + id);
                });
    }

    public ResponseEntity<ApiResponse<Boolean>> isUserEmailVerified(Long userId) {
        log.info("Checking if user email is verified for user ID: {}", userId);
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        EmailVerification verification = verifyByUserId(userId);
        boolean isVerified = verification.isVerified();
        log.info("Email verification status for user ID {}: {}", userId, isVerified);
        return ResponseEntity.ok(ApiResponse.success("Email verification status retrieved", isVerified));
    }

    public EmailVerification verifyByUserId(Long userId) {
        log.info("Verifying email by user ID: {}", userId);
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        return emailVerificationRepository.findByUserId(userId)
                .orElseThrow(() -> new InvalidValidationCodeException("No verification found for this user ID"));
    }

    private String generateVerificationCode(User user) {
        String code = generateVerificationCode();
        EmailVerification verification = new EmailVerification();
        verification.setUserId(user.getId());
        verification.setVerificationCode(code);
        verification.setExpiresAt(Timestamp.from(Instant.now().plus(Duration.ofHours(24))));
        emailVerificationRepository.save(verification);
        return code;
    }

    public void verifyEmail(VerifyEmailRequestDto dto) {
        EmailVerification verification = emailVerificationRepository
                .findByUserIdAndVerificationCode(dto.getUserId(), dto.getVerificationCode())
                .orElseThrow(() -> new InvalidValidationCodeException("Invalid verification code"));
        if (verification.isVerified()) {
            throw new InvalidValidationCodeException("Email already verified");
        }
        if (verification.getExpiresAt().before(new Timestamp(System.currentTimeMillis()))) {
            throw new InvalidValidationCodeException("Verification code expired");
        }
        verification.setVerified(true);
        verification.setVerifiedAt(new Timestamp(System.currentTimeMillis()));
        emailVerificationRepository.save(verification);
    }

    public String generateVerificationCode() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }

    public ResponseEntity<ApiResponse<Object>> resendVerificationEmail(Long userId) {
        log.info("Resending verification email for user ID: {}", userId);
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        EmailVerification verification = verifyByUserId(userId);
        User user = getUserById(userId);
        if (verification.isVerified()) {
            log.warn("Email already verified for user ID: {}", userId);
            return ResponseEntity.ok(ApiResponse.success("Email already verified"));
        }
        String code = generateVerificationCode(user);
        emailService.sendVerificationEmail(user.getEmail(), code);
        return ResponseEntity.ok(ApiResponse.success("Verification email resent successfully"));
    }
}

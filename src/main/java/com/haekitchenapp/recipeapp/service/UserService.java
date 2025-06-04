package com.haekitchenapp.recipeapp.service;

import com.haekitchenapp.recipeapp.entity.EmailVerification;
import com.haekitchenapp.recipeapp.entity.User;
import com.haekitchenapp.recipeapp.exception.InvalidCredentialsException;
import com.haekitchenapp.recipeapp.exception.InvalidValidationCodeException;
import com.haekitchenapp.recipeapp.exception.UserEmailExistsException;
import com.haekitchenapp.recipeapp.exception.UserNotFoundException;
import com.haekitchenapp.recipeapp.model.request.email.VerifyEmailRequestDto;
import com.haekitchenapp.recipeapp.model.request.user.LoginRequestDto;
import com.haekitchenapp.recipeapp.model.request.user.UserRequestDto;
import com.haekitchenapp.recipeapp.model.response.ApiResponse;
import com.haekitchenapp.recipeapp.model.response.user.LoginResponseDto;
import com.haekitchenapp.recipeapp.model.response.user.UserResponseDto;
import com.haekitchenapp.recipeapp.repository.EmailVerificationRepository;
import com.haekitchenapp.recipeapp.repository.UserRepository;
import com.haekitchenapp.recipeapp.utility.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
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

    public ResponseEntity<ApiResponse<UserResponseDto>> createUser(UserRequestDto dto) {
        log.info("Received request to create user: {}", dto);
        if (getUserByEmail(dto.getEmail()).isPresent()) {
            log.error("User with email {} already exists", dto.getEmail());
            throw new UserEmailExistsException("User with this email already exists");
        }
        log.info("User with email {} does not exist, proceeding to create user", dto.getEmail());
        User user = userMapper.mapToUser(dto);
        saveUser(user);

        emailService.sendVerificationEmail(user.getEmail(), generateVerificationCode(user));

        return ResponseEntity.ok(ApiResponse.success(
                "User created successfully",
                new UserResponseDto(user.getId(), user.getEmail(), user.getUsername()))
        );
    }

    public void saveUser(User user) {
        try {
            userRepository.save(user);
            log.info("User saved with ID: {}", user.getId());
        } catch (DataIntegrityViolationException e) {
            log.error("Error saving user: {}", e.getMessage());
            throw new UserEmailExistsException("User with this email already exists");
        }
    }

    public ResponseEntity<ApiResponse<LoginResponseDto>> login(LoginRequestDto dto) {
        log.info("Attempting to login user with email: {}", dto.getEmail());
        if (dto.getEmail() == null || dto.getPassword() == null || dto.getEmail().isBlank() || dto.getPassword().isBlank()) {
            throw new InvalidCredentialsException("Email and password must not be null or blank for login");
        }

        Optional<User> optionalUser = getUserByEmail(dto.getEmail());
        if (optionalUser.isEmpty()) {
            log.error("User with email {} not found", dto.getEmail());
            throw new UserNotFoundException("User not found with this email");
        }

        User user = optionalUser.get();
        if (!BCrypt.checkpw(dto.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("User with email exists but invalid credentials");
        }

        return ResponseEntity.ok(ApiResponse.success(
                "User logged in successfully",
                new LoginResponseDto(user.getId(), user.getEmail(), user.getUsername())
        ));
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

    public Optional<User> getUserByEmail(String email) {
        log.info("Fetching user by email: {}", email);
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be null or blank");
        }
        return userRepository.findByEmail(email);
    }

    public User getUserById(Long userId) {
        log.info("Fetching user by ID: {}", userId);
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with this ID"));
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

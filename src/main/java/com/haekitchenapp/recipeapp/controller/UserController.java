package com.haekitchenapp.recipeapp.controller;

import com.haekitchenapp.recipeapp.exception.InvalidCredentialsException;
import com.haekitchenapp.recipeapp.exception.UserNotFoundException;
import com.haekitchenapp.recipeapp.model.request.email.VerifyEmailRequestDto;
import com.haekitchenapp.recipeapp.model.request.user.LoginRequestDto;
import com.haekitchenapp.recipeapp.model.request.user.UserRequestDto;
import com.haekitchenapp.recipeapp.model.response.ApiResponse;
import com.haekitchenapp.recipeapp.model.response.user.LoginResponseDto;
import com.haekitchenapp.recipeapp.model.response.user.UserResponseDto;
import com.haekitchenapp.recipeapp.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/isVerified/{userId}")
    public ResponseEntity<ApiResponse<Boolean>> isEmailVerified(@PathVariable Long userId) {
        log.info("Received request to check if email is verified for user ID: {}", userId);
        return userService.isUserEmailVerified(userId);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Object>> verifyEmail(@Valid @RequestBody VerifyEmailRequestDto dto) {
        userService.verifyEmail(dto);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully"));
    }

    @PostMapping("/resend-verification/{userId}")
    public ResponseEntity<ApiResponse<Object>> resendVerificationEmail(@PathVariable Long userId) {
        log.info("Received request to resend verification email for user ID: {}", userId);
        return userService.resendVerificationEmail(userId);
    }


}

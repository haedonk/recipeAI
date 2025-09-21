package com.haekitchenapp.recipeapp.controller;

import com.haekitchenapp.recipeapp.model.request.email.VerifyEmailRequestDto;
import com.haekitchenapp.recipeapp.model.response.ApiResponse;
import com.haekitchenapp.recipeapp.service.JwtTokenService;
import com.haekitchenapp.recipeapp.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtTokenService jwtTokenService;

    @GetMapping("/isVerified")
    public ResponseEntity<ApiResponse<Boolean>> isEmailVerified(HttpServletRequest request) {
        Long userId = jwtTokenService.getUserIdFromRequest(request);
        log.info("Received request to check if email is verified for user ID: {}", userId);
        return userService.isUserEmailVerified(userId);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Object>> verifyEmail(@Valid @RequestBody VerifyEmailRequestDto dto) {
        userService.verifyEmail(dto);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Object>> resendVerificationEmail(HttpServletRequest request) {
        Long userId = jwtTokenService.getUserIdFromRequest(request);
        log.info("Received request to resend verification email for user ID: {}", userId);
        return userService.resendVerificationEmail(userId);
    }
}

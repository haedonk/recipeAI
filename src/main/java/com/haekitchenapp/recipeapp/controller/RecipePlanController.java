package com.haekitchenapp.recipeapp.controller;

import com.haekitchenapp.recipeapp.model.request.recipe.BulkRecipePlanRequest;
import com.haekitchenapp.recipeapp.model.response.ApiResponse;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipePlanResponse;
import com.haekitchenapp.recipeapp.service.JwtTokenService;
import com.haekitchenapp.recipeapp.service.RecipePlanService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing recipe plans (meal planning)
 */
@RestController
@RequestMapping("/api/meal-plans")
@RequiredArgsConstructor
@Slf4j
public class RecipePlanController {

    private final RecipePlanService recipePlanService;
    private final JwtTokenService jwtTokenService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecipePlanResponse>>> getUserRecipePlans(
            @RequestParam String startDate,
            @RequestParam String endDate,
            HttpServletRequest request
    ) {
        Long userId = jwtTokenService.getUserIdFromRequest(request);
        log.info("Received request to get recipe plans for user ID: {}", userId);
        return recipePlanService.getPlansInDateRange(userId, startDate, endDate);
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<RecipePlanResponse>>> createBulkRecipePlans(
            @Valid @RequestBody List<BulkRecipePlanRequest> bulkRequests,
            HttpServletRequest request) {

        Long userId = jwtTokenService.getUserIdFromRequest(request);
        log.info("Received request to create {} recipe plans in bulk for user ID: {}",
                bulkRequests.size(), userId);
        return recipePlanService.createBulkRecipePlans(userId, bulkRequests);
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResponse<Void>> deleteBulkRecipePlans(
            @RequestBody List<Long> planIds,
            HttpServletRequest request) {

        Long userId = jwtTokenService.getUserIdFromRequest(request);
        log.info("Received request to delete {} recipe plans in bulk for user ID: {}",
                planIds.size(), userId);
        return recipePlanService.deleteBulkRecipePlans(userId, planIds);
    }

    @PatchMapping("/bulk/toggle-saved")
    public ResponseEntity<ApiResponse<String>> toggleSavedStatusBulk(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam Boolean saved,
            HttpServletRequest request) {

        Long userId = jwtTokenService.getUserIdFromRequest(request);
        log.info("Received request to toggle saved status for recipe plans from {} to {} for user ID: {}",
                startDate, endDate, userId);
        return recipePlanService.toggleSavedStatusBulk(userId, startDate, endDate, saved);
    }
}

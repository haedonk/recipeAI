package com.haekitchenapp.recipeapp.controller;

import com.haekitchenapp.recipeapp.model.request.recipe.BulkRecipePlanRequest;
import com.haekitchenapp.recipeapp.model.response.ApiResponse;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipePlanResponse;
import com.haekitchenapp.recipeapp.service.RecipePlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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


    @GetMapping
    public ResponseEntity<ApiResponse<List<RecipePlanResponse>>> getUserRecipePlans(
            @RequestParam String startDate,
            @RequestParam String endDate
    ) {
        // Get the user ID from the security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();

        log.info("Received request to get recipe plans for user name: {}", userName);

        return recipePlanService.getPlansInDateRange(userName, startDate, endDate);
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<RecipePlanResponse>>> createBulkRecipePlans(
            @Valid @RequestBody List<BulkRecipePlanRequest> bulkRequests) {

        // Get the user ID from the security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();

        log.info("Received request to create {} recipe plans in bulk for user name: {}",
                bulkRequests.size(), userName);

        return recipePlanService.createBulkRecipePlans(userName, bulkRequests);
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResponse<Void>> deleteBulkRecipePlans(
            @RequestBody List<Long> planIds) {

        // Get the user ID from the security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();

        log.info("Received request to delete {} recipe plans in bulk for user name: {}",
                planIds.size(), userName);

        return recipePlanService.deleteBulkRecipePlans(userName, planIds);
    }

    @PatchMapping("/bulk/toggle-saved")
    public ResponseEntity<ApiResponse<String>> toggleSavedStatusBulk(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam Boolean saved) {

        // Get the user ID from the security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();

        log.info("Received request to toggle saved status for recipe plans from {} to {} for user name: {}",
                startDate, endDate, userName);

        return recipePlanService.toggleSavedStatusBulk(userName, startDate, endDate, saved);
    }
}

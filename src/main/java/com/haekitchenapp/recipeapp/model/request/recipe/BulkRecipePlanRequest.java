package com.haekitchenapp.recipeapp.model.request.recipe;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BulkRecipePlanRequest {
    @NotNull(message = "Plan date is required")
    @FutureOrPresent(message = "Plan date must be today or in the future")
    private LocalDate planDate;

    @NotNull(message = "Meal type ID is required")
    private Short mealTypeId;

    @NotNull(message = "Recipe ID is required")
    private Long recipeId;

    @Size(max = 100, message = "Custom title cannot exceed 100 characters")
    private String customTitle; // optional

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes; // optional
}
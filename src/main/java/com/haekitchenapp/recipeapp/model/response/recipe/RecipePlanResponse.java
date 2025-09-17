package com.haekitchenapp.recipeapp.model.response.recipe;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipePlanResponse {

    private Long id;

    private String username;

    // Date information
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate planDate;

    private String mealTypeName;

    // Recipe basic details (if recipe is set)
    private Long recipeId;
    private String recipeTitle;

    // Custom plan details
    private String customTitle;
    private String notes;
    private Boolean saved;
}

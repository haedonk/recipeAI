package com.haekitchenapp.recipeapp.model.request.recipeStage;

import com.haekitchenapp.recipeapp.model.request.recipe.RecipeIngredientRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeStageRequest {
    private Long id;

    @NotNull
    private Long createdBy;

    @NotBlank
    private String title;

    @NotBlank
    private String instructions;


    @Size(min = 1, message = "At least one ingredient is required")
    private Set<RecipeIngredientRequest> ingredients;

    private Integer prepTime;
    private Integer cookTime;
    private Integer servings;
}

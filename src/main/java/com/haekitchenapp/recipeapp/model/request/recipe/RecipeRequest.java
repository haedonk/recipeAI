package com.haekitchenapp.recipeapp.model.request.recipe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeRequest {
    private Long id;

    @NotBlank
    private String title;

    @NotBlank
    private String instructions;

    private String imageUrl;

    @Size(min = 1, message = "At least one ingredient is required")
    private Set<RecipeIngredientRequest> ingredients;

    private Integer prepTime;
    private Integer cookTime;
    private Integer servings;
}

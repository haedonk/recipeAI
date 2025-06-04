package com.haekitchenapp.recipeapp.model.response.recipe;

import com.haekitchenapp.recipeapp.model.request.recipe.RecipeIngredientRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeResponse {
    private Long id;

    private String title;

    private String instructions;

    private String imageUrl;

    private Set<RecipeIngredientRequest> ingredients;

    private Integer prepTime;
    private Integer cookTime;
    private Integer servings;
}

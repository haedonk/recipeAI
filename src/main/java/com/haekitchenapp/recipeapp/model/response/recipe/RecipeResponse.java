package com.haekitchenapp.recipeapp.model.response.recipe;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeResponse {
    private Long id;

    private Long createdBy;

    private String title;

    private String instructions;

    private String summary;

    private String imageUrl;

    private Set<RecipeIngredientResponse> ingredients;

    private Integer prepTime;
    private Integer cookTime;
    private Integer servings;
}

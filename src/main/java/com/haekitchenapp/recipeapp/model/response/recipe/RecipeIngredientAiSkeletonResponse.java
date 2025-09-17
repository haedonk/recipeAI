package com.haekitchenapp.recipeapp.model.response.recipe;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecipeIngredientAiSkeletonResponse {
    private String name;

    private String quantity;

    private String unit;
}
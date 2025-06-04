package com.haekitchenapp.recipeapp.model.response.recipe;

import lombok.Data;

@Data
public class RecipeIngredientResponse {
    private Long id;
    private String name;

    private String quantity;

    private String unit;
}
package com.haekitchenapp.recipeapp.model.response.recipe;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecipeIngredientResponse {
    private Long id;
    private String name;

    private String quantity;

    private Float quantityRaw;

    private String unit;
}
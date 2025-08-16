package com.haekitchenapp.recipeapp.model.request.recipe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeIngredientRequest {
    private Long id;
    @NotBlank
    private String name;

    @NotBlank
    private String quantity;

    @NotNull
    private Integer unitId;
}
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

    public RecipeIngredientRequest(Long id, @NotBlank String name, @NotBlank String quantity, Long unitId) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unitId = unitId;
    }

    private Long id;
    @NotBlank
    private String name;

    @NotBlank
    private String quantity;

    private Long unitId;

    private String unitName;
}
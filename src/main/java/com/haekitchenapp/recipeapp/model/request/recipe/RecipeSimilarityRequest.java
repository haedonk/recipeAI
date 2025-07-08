package com.haekitchenapp.recipeapp.model.request.recipe;

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
public class RecipeSimilarityRequest {

    @NotBlank
    private String title;

    private String cuisine;

    private String includeIngredients;

    private String excludeIngredients;

    @NotBlank
    private String mealType;

    @NotBlank
    private String detailLevel;

    @NotNull
    private Integer limit;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(title != null) {
            sb.append("Title: ").append(title).append(", ");
        }
        if(cuisine != null) {
            sb.append("Cuisine: ").append(cuisine).append(", ");
        }
        if(includeIngredients != null) {
            sb.append("Include Ingredients: ").append(includeIngredients).append(", ");
        }
        if(excludeIngredients != null) {
            sb.append("Exclude Ingredients: ").append(excludeIngredients).append(", ");
        }
        if(mealType != null) {
            sb.append("Meal Type: ").append(mealType).append(", ");
        }
        if(detailLevel != null) {
            sb.append("Detail Level: ").append(detailLevel);
        }
        return sb.toString();
    }
}

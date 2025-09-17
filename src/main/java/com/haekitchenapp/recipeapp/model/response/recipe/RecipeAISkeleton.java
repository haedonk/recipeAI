package com.haekitchenapp.recipeapp.model.response.recipe;

import com.haekitchenapp.recipeapp.model.request.recipe.RecipeIngredientRequest;
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeAISkeleton {
    private String title;

    private String instructions;

    private String summary;

    private Set<RecipeIngredientAiSkeletonResponse> ingredients = new LinkedHashSet<>();

    private Integer prepTime;
    private Integer cookTime;
    private Integer servings;

    public RecipeRequest toRecipeRequest(Long userId, Long cleanedFrom) {
        RecipeRequest request = new RecipeRequest();
        request.setCreatedBy(userId);
        request.setTitle(this.title);
        request.setInstructions(this.instructions);
        request.setSummary(this.summary);
        request.setPrepTime(this.prepTime);
        request.setCookTime(this.cookTime);
        request.setServings(this.servings);
        request.setAiGenerated(true);
        request.setCleanedFrom(cleanedFrom);

        Set<RecipeIngredientRequest> ingredientRequests = this.ingredients.stream()
                .map(ingredient -> {
                    RecipeIngredientRequest riRequest = new RecipeIngredientRequest();
                    riRequest.setName(ingredient.getName());
                    riRequest.setQuantity(ingredient.getQuantity());
                    riRequest.setUnitName(ingredient.getUnit());
                    return riRequest;
                })
                .collect(Collectors.toSet());
        request.setIngredients(ingredientRequests);
        return request;
    }
}

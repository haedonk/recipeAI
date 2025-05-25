package com.haekitchenapp.recipeapp.utility;

import com.haekitchenapp.recipeapp.entity.*;
import com.haekitchenapp.recipeapp.model.request.RecipeIngredientRequest;
import com.haekitchenapp.recipeapp.model.request.RecipeRequest;
import com.haekitchenapp.recipeapp.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RecipeMapper {

    private final IngredientRepository ingredientRepository;

    public Recipe toEntity(RecipeRequest request) {
        Recipe recipe = new Recipe();
        recipe.setTitle(request.getTitle());
        recipe.setInstructions(request.getInstructions());
        recipe.setPrepTime(request.getPrepTime());
        recipe.setCookTime(request.getCookTime());
        recipe.setServings(request.getServings());

        Set<RecipeIngredient> ingredients = request.getIngredients().stream()
            .map(ri -> mapToRecipeIngredient(ri, recipe))
            .collect(Collectors.toSet());

        recipe.setIngredients(ingredients);
        return recipe;
    }

    private RecipeIngredient mapToRecipeIngredient(RecipeIngredientRequest riRequest, Recipe recipe) {
        Ingredient ingredient = ingredientRepository
                .findByNameIgnoreCase(riRequest.getName())
                .orElseGet(() -> {
                    Ingredient newIng = new Ingredient();
                    newIng.setName(riRequest.getName());
                    return ingredientRepository.save(newIng);
                });

        RecipeIngredient ri = new RecipeIngredient();
        ri.setRecipe(recipe);
        ri.setIngredient(ingredient);
        ri.setQuantity(riRequest.getQuantity());
        ri.setUnit(riRequest.getUnit());

        return ri;
    }

    public Recipe toEntity(Recipe recipe, RecipeRequest request) {
        RecipeRequest recipeRequest = new RecipeRequest();
        recipeRequest.setId(recipe.getId());
        recipeRequest.setTitle(request.getTitle() != null ? request.getTitle() : recipe.getTitle());
        recipeRequest.setInstructions(request.getInstructions() != null ? request.getInstructions() : recipe.getInstructions());
        recipeRequest.setPrepTime(request.getPrepTime() != null ? request.getPrepTime() : recipe.getPrepTime());
        recipeRequest.setCookTime(request.getCookTime() != null ? request.getCookTime() : recipe.getCookTime());
        recipeRequest.setServings(request.getServings() != null ? request.getServings() : recipe.getServings());
        recipeRequest.setIngredients(request.getIngredients() != null ? request.getIngredients() : recipe.getIngredients().stream()
                .map(ri -> new RecipeIngredientRequest(ri.getId(), ri.getIngredient().getName(), ri.getQuantity(), ri.getUnit()))
                .collect(Collectors.toSet()));
        return toEntity(recipeRequest);
    }
}

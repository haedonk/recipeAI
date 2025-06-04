package com.haekitchenapp.recipeapp.utility;

import com.haekitchenapp.recipeapp.entity.*;
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeIngredientRequest;
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeRequest;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeDetailsDto;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeResponse;
import com.haekitchenapp.recipeapp.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
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

    public RecipeResponse toRecipeResponse(Recipe recipe) {
        RecipeResponse recipeResponse = new RecipeResponse();
        recipeResponse.setId(recipe.getId());
        recipeResponse.setTitle(recipe.getTitle());
        recipeResponse.setInstructions(recipe.getInstructions());
        recipeResponse.setPrepTime(recipe.getPrepTime());
        recipeResponse.setCookTime(recipe.getCookTime());
        recipeResponse.setServings(recipe.getServings());

        Set<RecipeIngredientRequest> ingredients = recipe.getIngredients().stream()
                .map(this::toRecipeIngredientRequest)
                .collect(Collectors.toSet());

        recipeResponse.setIngredients(ingredients);
        return recipeResponse;
    }

    private RecipeIngredientRequest toRecipeIngredientRequest(RecipeIngredient ri) {
        return new RecipeIngredientRequest(
                ri.getId(),
                ri.getIngredient().getName(),
                ri.getQuantity(),
                ri.getUnit()
        );
    }

    public RecipeDetailsDto toLlmDetailsDto(Recipe recipe) {
        List<String> ingredients = recipe.getIngredients().stream()
                .map(ri -> ri.getIngredient().getName())
                .collect(Collectors.toList());

        return new RecipeDetailsDto(recipe.getTitle(), ingredients, recipe.getInstructions());
    }
}

package com.haekitchenapp.recipeapp.utility;

import com.haekitchenapp.recipeapp.entity.*;
import com.haekitchenapp.recipeapp.exception.UnitNotFoundException;
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeIngredientRequest;
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeRequest;
import com.haekitchenapp.recipeapp.model.request.recipeStage.RecipeStageRequest;
import com.haekitchenapp.recipeapp.model.response.recipe.*;
import com.haekitchenapp.recipeapp.repository.IngredientRepository;
import com.haekitchenapp.recipeapp.service.IngredientService;
import com.haekitchenapp.recipeapp.service.UnitService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RecipeMapper {

    private final UnitService unitService;
    private final IngredientService ingredientService;


    public Recipe toEntity(RecipeRequest request) {
        Recipe recipe = new Recipe();
        recipe.setId(request.getId());
        recipe.setCreatedBy(request.getCreatedBy());
        recipe.setTitle(request.getTitle());
        recipe.setInstructions(request.getInstructions());
        recipe.setSummary(request.getSummary() != null ? request.getSummary() : "");
        recipe.setEmbedding(request.getEmbedding() != null ? request.getEmbedding() : null);
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
        Ingredient ingredient = ingredientService.getIngredientElseInsert(riRequest.getName());

        RecipeIngredient ri = new RecipeIngredient();
        ri.setRecipe(recipe);
        ri.setIngredient(ingredient);
        ri.setQuantity(riRequest.getQuantity());

        // Validate unit ID exists before setting it
        Long unitId = riRequest.getUnitId();
        if (unitId != null && !unitService.existsById(unitId)) {
            throw new UnitNotFoundException("Unit not found with ID: " + unitId);
        }
        ri.setUnitId(unitId);

        return ri;
    }


    public RecipeStage toEntity(RecipeStageRequest request) {
        RecipeStage recipe = new RecipeStage();
        recipe.setId(request.getId());
        recipe.setCreatedBy(request.getCreatedBy());
        recipe.setTitle(request.getTitle());
        recipe.setInstructions(request.getInstructions());
        recipe.setPrepTime(request.getPrepTime());
        recipe.setCookTime(request.getCookTime());
        recipe.setServings(request.getServings());

        Set<RecipeStageIngredient> ingredients = request.getIngredients().stream()
                .map(ri -> mapToRecipeIngredient(ri, recipe))
                .collect(Collectors.toSet());

        recipe.setIngredients(ingredients);
        return recipe;
    }

    private RecipeStageIngredient mapToRecipeIngredient(RecipeIngredientRequest riRequest, RecipeStage recipe) {
        Ingredient ingredient = ingredientService.getIngredientElseInsert(riRequest.getName());

        RecipeStageIngredient ri = new RecipeStageIngredient();
        ri.setRecipe(recipe);
        ri.setIngredient(ingredient);
        ri.setQuantity(riRequest.getQuantity());

        // Validate unit ID exists before setting it
        Long unitId = riRequest.getUnitId();
        if (unitId != null && !unitService.existsById(unitId)) {
            throw new UnitNotFoundException("Unit not found with ID: " + unitId);
        }
        ri.setUnitId(unitId);

        return ri;
    }

    public RecipeStage toEntity(RecipeStage recipe, RecipeStageRequest request) {
        RecipeStageRequest recipeRequest = new RecipeStageRequest();
        recipeRequest.setId(recipe.getId());
        recipeRequest.setCreatedBy(recipe.getCreatedBy());
        recipeRequest.setTitle(request.getTitle() != null ? request.getTitle() : recipe.getTitle());
        recipeRequest.setInstructions(request.getInstructions() != null ? request.getInstructions() : recipe.getInstructions());
        recipeRequest.setPrepTime(request.getPrepTime() != null ? request.getPrepTime() : recipe.getPrepTime());
        recipeRequest.setCookTime(request.getCookTime() != null ? request.getCookTime() : recipe.getCookTime());
        recipeRequest.setServings(request.getServings() != null ? request.getServings() : recipe.getServings());
        recipeRequest.setIngredients(request.getIngredients() != null ? request.getIngredients() : recipe.getIngredients().stream()
                .map(ri -> new RecipeIngredientRequest(ri.getId(), ri.getIngredient().getName(), ri.getQuantity(), ri.getUnitId()))
                .collect(Collectors.toSet()));
        return toEntity(recipeRequest);
    }


    public Recipe toEntity(Recipe recipe, RecipeRequest request) {
        RecipeRequest recipeRequest = new RecipeRequest();
        recipeRequest.setId(recipe.getId());
        recipeRequest.setCreatedBy(recipe.getCreatedBy());
        recipeRequest.setTitle(request.getTitle() != null ? request.getTitle() : recipe.getTitle());
        recipeRequest.setInstructions(request.getInstructions() != null ? request.getInstructions() : recipe.getInstructions());
        recipeRequest.setSummary(request.getSummary() != null ? request.getSummary() : recipe.getSummary());
        recipeRequest.setEmbedding(request.getEmbedding() != null ? request.getEmbedding() : recipe.getEmbedding());
        recipeRequest.setPrepTime(request.getPrepTime() != null ? request.getPrepTime() : recipe.getPrepTime());
        recipeRequest.setCookTime(request.getCookTime() != null ? request.getCookTime() : recipe.getCookTime());
        recipeRequest.setServings(request.getServings() != null ? request.getServings() : recipe.getServings());
        recipeRequest.setIngredients(request.getIngredients() != null ? request.getIngredients() : recipe.getIngredients().stream()
                .map(ri -> new RecipeIngredientRequest(ri.getId(), ri.getIngredient().getName(), ri.getQuantity(), ri.getUnitId()))
                .collect(Collectors.toSet()));
        return toEntity(recipeRequest);
    }

    public RecipeResponse toRecipeResponse(Recipe recipe, boolean raw) {
        RecipeResponse recipeResponse = new RecipeResponse();
        recipeResponse.setId(recipe.getId());
        recipeResponse.setCreatedBy(recipe.getCreatedBy());
        recipeResponse.setTitle(recipe.getTitle());
        recipeResponse.setInstructions(recipe.getInstructions());
        recipeResponse.setSummary(recipe.getSummary());
        recipeResponse.setPrepTime(recipe.getPrepTime());
        recipeResponse.setCookTime(recipe.getCookTime());
        recipeResponse.setServings(recipe.getServings());

        Set<RecipeIngredientResponse> ingredients = recipe.getIngredients().stream()
                .map(ingredientRecipe -> toRecipeIngredientRequest(ingredientRecipe, raw))
                .collect(Collectors.toSet());

        recipeResponse.setIngredients(ingredients);
        return recipeResponse;
    }

    private RecipeIngredientResponse toRecipeIngredientRequest(RecipeIngredient ri, boolean raw) {
        return new RecipeIngredientResponse(
                ri.getId(),
                ri.getIngredient().getName(),
                raw ?  ri.getQuantity().toString() : normalizeQuantity(ri.getQuantity().toString()),
                raw ?  ri.getQuantity() : null,
                getUnit(ri.getUnitId())
        );
    }

    private @NotNull String getUnit(Long unitId) {
        return unitService.getUnitNameById(unitId);
    }

    private @NotBlank String normalizeQuantity(String quantity) {
        int numerator = (int) (Double.parseDouble(quantity) * 10);
        int denominator = 10;
        if (numerator % denominator == 0) {
            return String.valueOf(numerator / denominator);
        } else {
            return numerator + "/" + denominator;
        }
    }

    public RecipeDetailsDto toSimpleDto(RecipeSummaryProjection recipe, List<Long> ingredients) {

        List<String> ingredientNames = ingredients.stream()
                .map(ingredientService::getIngredientNameById).toList();

        return new RecipeDetailsDto(recipe.getTitle(), ingredientNames, recipe.getInstructions());
    }

    public RecipeDetailsDto toLlmDetailsDto(Recipe recipe) {
        List<String> ingredients = recipe.getIngredients().stream()
                .map(ri -> ri.getIngredient().getName())
                .collect(Collectors.toList());

        return new RecipeDetailsDto(recipe.getTitle(), ingredients, recipe.getInstructions());
    }
}

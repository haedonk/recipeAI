package com.haekitchenapp.recipeapp.utility;

import com.haekitchenapp.recipeapp.entity.Ingredient;
import com.haekitchenapp.recipeapp.entity.Recipe;
import com.haekitchenapp.recipeapp.entity.RecipeIngredient;
import com.haekitchenapp.recipeapp.entity.Unit;
import com.haekitchenapp.recipeapp.exception.UnitNotFoundException;
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeIngredientRequest;
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeRequest;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeDetailsDto;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeIngredientResponse;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeResponse;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeSummaryProjection;
import com.haekitchenapp.recipeapp.service.IngredientService;
import com.haekitchenapp.recipeapp.service.UnitService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
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
        recipe.setAiGenerated(request.getAiGenerated() != null ? request.getAiGenerated() : false);
        recipe.setCleanedFrom(request.getCleanedFrom());

        if(Boolean.TRUE.equals(request.getAiGenerated()))
            unitService.persistAiGeneratedUnits(request.getIngredients());

        Set<RecipeIngredient> ingredients = request.getIngredients().stream()
            .map(ri -> mapToRecipeIngredient(ri, recipe, request.getAiGenerated()))
            .collect(Collectors.toSet());

        recipe.setIngredients(ingredients);
        return recipe;
    }

    private RecipeIngredient mapToRecipeIngredient(RecipeIngredientRequest riRequest, Recipe recipe, Boolean aiGenerated) {
        Ingredient ingredient = ingredientService.getIngredientElseInsert(riRequest.getName());

        RecipeIngredient ri = new RecipeIngredient();
        ri.setRecipe(recipe);
        ri.setIngredient(ingredient);
        ri.setQuantity(convertQuantityToFloat(riRequest.getQuantity()));

        if(Objects.isNull(riRequest.getUnitId()) && Objects.isNull(riRequest.getUnitName())){
            throw new UnitNotFoundException("Unit ID and Unit Name cannot be null");
        }

        // Validate unit ID exists before setting it
        Long unitId = riRequest.getUnitId();

        try {
            if (Objects.nonNull(unitId) && !unitService.existsById(unitId) && !aiGenerated) {
                throw new UnitNotFoundException("Unit not found with ID: " + unitId);
            } else if (aiGenerated){
                Unit unit = unitService.getUnitByName(riRequest.getUnitName());
                unitId = Objects.nonNull(unit) ? unit.getId() : null;
            }
        } catch (Exception e){
            log.info("Exception occurred while validating unit ID {} and name {}: {}", unitId, riRequest.getUnitName(), e.getMessage());
            throw e;
        }

        ri.setUnitId(unitId);

        return ri;
    }

    /**
     * Converts a string quantity to float value, handling both fractions and decimals.
     * @param quantity The quantity as a string (e.g., "1/2", "0.5", "2")
     * @return The float value of the quantity, or 0.0f if parsing fails
     */
    public float convertQuantityToFloat(String quantity) {
        if (quantity == null || quantity.trim().isEmpty() || "to taste".equalsIgnoreCase(quantity.trim())) {
            return 0.0f;
        }

        quantity = quantity.trim();

        try {
            // Check if it's a fraction (contains /)
            if (quantity.contains("/")) {
                String[] parts = quantity.split("/");
                if (parts.length == 2) {
                    float numerator = Float.parseFloat(parts[0].trim());
                    float denominator = Float.parseFloat(parts[1].trim());
                    if (denominator == 0) {
                        return 0.0f; // Avoid division by zero
                    }
                    return numerator / denominator;
                }
            }

            // It's already a decimal or whole number
            return Float.parseFloat(quantity);
        } catch (NumberFormatException e) {
            // If parsing fails, return 0
            return 0.0f;
        }
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
                .map(ri -> new RecipeIngredientRequest(ri.getId(), ri.getIngredient().getName(), ri.getQuantity().toString(), ri.getUnitId()))
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
        double value = Double.parseDouble(quantity);

        // If it's a whole number, return it as is
        if (Math.floor(value) == value) {
            return String.valueOf((int)value);
        }

        // Convert to a fraction and reduce
        int precision = 1000; // Increased precision for better fraction approximation
        int gcd = gcd((int)(value * precision), precision);
        int numerator = (int)(value * precision) / gcd;
        int denominator = precision / gcd;

        // If the numerator is divisible by the denominator, return as whole number
        if (numerator % denominator == 0) {
            return String.valueOf(numerator / denominator);
        } else {
            return numerator + "/" + denominator;
        }
    }

    /**
     * Calculate the greatest common divisor (GCD) using Euclidean algorithm
     * for reducing fractions to their simplest form.
     *
     * @param a First number
     * @param b Second number
     * @return The greatest common divisor
     */
    private int gcd(int a, int b) {
        return b == 0 ? Math.abs(a) : gcd(b, a % b);
    }

    public RecipeDetailsDto toSimpleDto(RecipeSummaryProjection recipe, List<Long> ingredients, Long id) {

        List<String> ingredientNames = ingredients.stream()
                .map(ingredientService::getIngredientNameById).toList();

        return new RecipeDetailsDto(recipe.getTitle(), ingredientNames, recipe.getInstructions(), id);
    }

    public RecipeDetailsDto toLlmDetailsDto(Recipe recipe) {
        List<String> ingredients = recipe.getIngredients().stream()
                .map(ri -> ri.getIngredient().getName())
                .collect(Collectors.toList());

        return new RecipeDetailsDto(recipe.getTitle(), ingredients, recipe.getInstructions());
    }
}

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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.haekitchenapp.recipeapp.utility.QuantityUtils.floatToFraction;
import static com.haekitchenapp.recipeapp.utility.QuantityUtils.fractionToFloat;

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
        recipe.setEmbedding(request.getEmbedding());
        recipe.setPrepTime(request.getPrepTime());
        recipe.setCookTime(request.getCookTime());
        recipe.setServings(request.getServings());
        recipe.setAiGenerated(request.getAiGenerated() != null ? request.getAiGenerated() : false);
        recipe.setCleanedFrom(request.getCleanedFrom());

        Set<RecipeIngredientRequest> ingredientRequests = Optional.ofNullable(request.getIngredients())
                .orElseGet(Collections::emptySet);

        if(Boolean.TRUE.equals(request.getAiGenerated()) && !ingredientRequests.isEmpty()) {
            unitService.persistAiGeneratedUnits(ingredientRequests);
        }

        Set<RecipeIngredient> ingredients = ingredientRequests.stream()
            .map(ri -> mapToRecipeIngredient(ri, recipe, request.getAiGenerated()))
            .collect(Collectors.toSet());

        recipe.setIngredients(ingredients);
        return recipe;
    }

    private RecipeIngredient mapToRecipeIngredient(RecipeIngredientRequest riRequest, Recipe recipe, Boolean aiGenerated) {
        Ingredient ingredient = ingredientService.getIngredientElseInsert(riRequest.getName());
        if (ingredient == null) {
            ingredient = new Ingredient();
            ingredient.setName(riRequest.getName());
        }

        RecipeIngredient ri = new RecipeIngredient();
        ri.setRecipe(recipe);
        ri.setIngredient(ingredient);
        ri.setQuantity(fractionToFloat(riRequest.getQuantity()));

        String unitName = riRequest.getUnitName();
        Long unitId = riRequest.getUnitId();
        boolean aiGeneratedFlag = Boolean.TRUE.equals(aiGenerated);

        if(Objects.isNull(unitId) && (unitName == null || unitName.isBlank())){
            throw new UnitNotFoundException("Unit ID and Unit Name cannot be null");
        }

        try {
            if (Objects.isNull(unitId) && Objects.nonNull(unitName)) {
                Unit unit = unitService.getUnitByName(unitName);
                unitId = Objects.nonNull(unit) ? unit.getId() : null;
                if (unitId == null && !aiGeneratedFlag) {
                    throw new UnitNotFoundException("Unit not found with name: " + unitName);
                }
            } else if (Objects.nonNull(unitId) && !aiGeneratedFlag) {
                if (!unitService.existsById(unitId)) {
                    log.warn("Unit with ID {} not found while mapping ingredient '{}'", unitId, riRequest.getName());
                }
            }
        } catch (Exception e){
            log.info("Exception occurred while validating unit ID {} and name {}: {}", unitId, unitName, e.getMessage());
            throw e;
        }

        ri.setUnitId(unitId);

        return ri;
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

        Set<RecipeIngredient> recipeIngredients = Optional.ofNullable(recipe.getIngredients())
                .orElseGet(Collections::emptySet);

        Set<RecipeIngredientResponse> ingredients = recipeIngredients.stream()
                .map(ingredientRecipe -> toRecipeIngredientRequest(ingredientRecipe, raw))
                .collect(Collectors.toSet());

        recipeResponse.setIngredients(ingredients);
        return recipeResponse;
    }

    private RecipeIngredientResponse toRecipeIngredientRequest(RecipeIngredient ri, boolean raw) {
        String ingredientName = ri.getIngredient() != null ? ri.getIngredient().getName() : null;
        Float quantity = ri.getQuantity();
        String displayQuantity = null;
        if (quantity != null) {
            displayQuantity = raw ? quantity.toString() : floatToFraction(quantity);
        }
        Float numericQuantity = raw ? quantity : null;

        return new RecipeIngredientResponse(
                ri.getId(),
                ingredientName,
                displayQuantity,
                numericQuantity,
                getUnit(ri.getUnitId())
        );
    }

    private @NotNull String getUnit(Long unitId) {
        if (unitId == null) {
            return null;
        }
        String unitName = unitService.getUnitNameById(unitId);
        return unitName != null ? unitName : "Unknown Unit";
    }

    public RecipeDetailsDto toSimpleDto(RecipeSummaryProjection recipe, List<Long> ingredients, List<String> cuisines, Long id) {
        List<String> ingredientNames = resolveIngredientNames(ingredients);
        List<String> cuisineNames = Optional.ofNullable(cuisines).orElse(List.of());

        return new RecipeDetailsDto(recipe.getTitle(), ingredientNames, cuisineNames, recipe.getInstructions(), id);
    }

    public RecipeDetailsDto toDetailedDto(RecipeSummaryProjection recipe, List<Long> ingredients, List<String> cuisines, Long id) {
        List<String> ingredientNames = resolveIngredientNames(ingredients);
        List<String> cuisineNames = Optional.ofNullable(cuisines).orElse(List.of());

        return new RecipeDetailsDto(recipe.getTitle(), ingredientNames, cuisineNames, recipe.getInstructions(), id);
    }

    public RecipeDetailsDto toLlmDetailsDto(Recipe recipe) {
        List<String> ingredients = recipe.getIngredients().stream()
                .map(ri -> ri.getIngredient().getName())
                .collect(Collectors.toList());

        return new RecipeDetailsDto(recipe.getTitle(), ingredients, recipe.getInstructions());
    }

    private List<String> resolveIngredientNames(List<Long> ingredientIds) {
        return Optional.ofNullable(ingredientIds).orElse(List.of())
                .stream()
                .map(id -> {
                    String name = ingredientService.getIngredientNameById(id);
                    return name != null ? name : "Unknown Ingredient";
                })
                .toList();
    }
}

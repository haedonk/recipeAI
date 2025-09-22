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
        ri.setQuantity(fractionToFloat(riRequest.getQuantity()));

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
                raw ?  ri.getQuantity().toString() : floatToFraction(ri.getQuantity()),
                raw ?  ri.getQuantity() : null,
                getUnit(ri.getUnitId())
        );
    }

    private @NotNull String getUnit(Long unitId) {
        return unitService.getUnitNameById(unitId);
    }

    public RecipeDetailsDto toSimpleDto(RecipeSummaryProjection recipe, List<Long> ingredients, List<String> cuisines, Long id) {

        List<String> ingredientNames = ingredients.stream()
                .map(ingredientService::getIngredientNameById).toList();

        return new RecipeDetailsDto(recipe.getTitle(), ingredientNames, cuisines, recipe.getInstructions(), id);
    }

    public RecipeDetailsDto toDetailedDto(RecipeSummaryProjection recipe, List<Long> ingredients, List<String> cuisines, Long id) {

        List<String> ingredientNames = ingredients.stream()
                .map(ingredientService::getIngredientNameById).toList();

        return new RecipeDetailsDto(recipe.getTitle(), ingredientNames, cuisines, recipe.getInstructions(), id);
    }

    public RecipeDetailsDto toLlmDetailsDto(Recipe recipe) {
        List<String> ingredients = recipe.getIngredients().stream()
                .map(ri -> ri.getIngredient().getName())
                .collect(Collectors.toList());

        return new RecipeDetailsDto(recipe.getTitle(), ingredients, recipe.getInstructions());
    }
}

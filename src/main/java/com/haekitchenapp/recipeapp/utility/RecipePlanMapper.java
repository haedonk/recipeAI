package com.haekitchenapp.recipeapp.utility;

import com.haekitchenapp.recipeapp.entity.MealType;
import com.haekitchenapp.recipeapp.entity.Recipe;
import com.haekitchenapp.recipeapp.entity.RecipePlan;
import com.haekitchenapp.recipeapp.entity.User;
import com.haekitchenapp.recipeapp.model.request.recipe.BulkRecipePlanRequest;
import com.haekitchenapp.recipeapp.model.response.RecipePlanSimple;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipePlanResponse;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto;
import com.haekitchenapp.recipeapp.repository.MealTypeRepository;
import com.haekitchenapp.recipeapp.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecipePlanMapper {

    private final MealTypeRepository mealTypeRepository;
    private final RecipeRepository recipeRepository;


    public List<RecipePlan> toEntity(List<BulkRecipePlanRequest> bulkPlanRequests, Long userId) {
        return bulkPlanRequests.stream().map(request -> {
            RecipePlan plan = new RecipePlan();
            // Create a User reference with just the ID (JPA will manage the relationship)
            User user = new User();
            user.setId(userId);
            plan.setUser(user);
            plan.setPlanDate(request.getPlanDate());

            // Set meal type
            MealType mealType = mealTypeRepository.getReferenceById(request.getMealTypeId());
            plan.setMealType(mealType);

            // Set recipe if provided
            if (request.getRecipeId() != null) {
                Recipe recipe = recipeRepository.getReferenceById(request.getRecipeId());
                log.info("Associating recipe ID: {} with plan on date: {}", recipe.getId(), request.getPlanDate());
                plan.setRecipe(recipe);
            }

            // Set custom title and notes if provided
            plan.setCustomTitle(request.getCustomTitle());
            plan.setNotes(request.getNotes());

            return plan;
        }).toList();
    }

    public List<RecipePlanResponse> toResponse(List<RecipePlan> recipePlans) {
        return recipePlans.stream().map(recipePlan -> {
            RecipePlanResponse response = new RecipePlanResponse();
            response.setId(recipePlan.getId());
            response.setUsername(recipePlan.getUser().getUsername());
            response.setPlanDate(recipePlan.getPlanDate());
            response.setMealTypeName(recipePlan.getMealType().getName());
            if (recipePlan.getRecipe() != null) {
                response.setRecipeId(recipePlan.getRecipe().getId());
                response.setRecipeTitle(recipePlan.getRecipe().getTitle());
            }
            response.setCustomTitle(recipePlan.getCustomTitle());
            response.setNotes(recipePlan.getNotes());
            response.setSaved(recipePlan.getSaved());
            return response;
        }).toList();
    }

    public List<RecipePlanResponse> toResponseSimple(List<RecipePlanSimple> recipePlans) {
        return recipePlans.stream().map(recipePlan -> {
            RecipePlanResponse response = new RecipePlanResponse();
            RecipeTitleDto recipeTitleDto = recipeRepository.findRecipeTitleDtoById(recipePlan.getRecipeId()).orElse(null);
            String mealTypeName = mealTypeRepository.findNameById(recipePlan.getMealTypeId()).orElse("Unknown");
            response.setId(recipePlan.getId());
            response.setPlanDate(recipePlan.getPlanDate());
            response.setMealTypeName(mealTypeName);
            if (recipeTitleDto != null) {
                response.setRecipeId(recipeTitleDto.getId());
                response.setRecipeTitle(recipeTitleDto.getTitle());
            } else {
                log.info("Recipe with ID {} not found for RecipePlan ID {}", recipePlan.getRecipeId(), recipePlan.getId());
            }
            response.setCustomTitle(recipePlan.getCustomTitle());
            response.setNotes(recipePlan.getNotes());
            response.setSaved(recipePlan.getSaved());
            return response;
        }).toList();
    }

}

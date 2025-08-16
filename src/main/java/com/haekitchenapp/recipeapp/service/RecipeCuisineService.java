package com.haekitchenapp.recipeapp.service;

import com.haekitchenapp.recipeapp.entity.Cuisine;
import com.haekitchenapp.recipeapp.entity.composite.RecipeCuisine;

import java.util.List;
import java.util.Set;

public interface RecipeCuisineService {

    /**
     * Associate a recipe with a cuisine
     * @param recipeId the recipe ID
     * @param cuisineId the cuisine ID
     * @return the created association
     */
    RecipeCuisine associateRecipeWithCuisine(Long recipeId, Integer cuisineId);

    /**
     * Associate a recipe with multiple cuisines
     * @param recipeId the recipe ID
     * @param cuisineIds set of cuisine IDs
     */
    void associateRecipeWithCuisines(Long recipeId, Set<Integer> cuisineIds);

    /**
     * Get all cuisine associations for a recipe
     * @param recipeId the recipe ID
     * @return list of RecipeCuisine associations
     */
    List<RecipeCuisine> getRecipeCuisines(Long recipeId);

    /**
     * Get all recipes associated with a cuisine
     * @param cuisineId the cuisine ID
     * @return list of RecipeCuisine associations
     */
    List<RecipeCuisine> getCuisineRecipes(Integer cuisineId);

    /**
     * Remove an association between a recipe and a cuisine
     * @param recipeId the recipe ID
     * @param cuisineId the cuisine ID
     */
    void removeRecipeCuisine(Long recipeId, Integer cuisineId);

    /**
     * Remove all cuisine associations for a recipe
     * @param recipeId the recipe ID
     */
    void removeAllRecipeCuisines(Long recipeId);

    /**
     * Check if a recipe is associated with a cuisine
     * @param recipeId the recipe ID
     * @param cuisineId the cuisine ID
     * @return true if associated, false otherwise
     */
    boolean isRecipeAssociatedWithCuisine(Long recipeId, Integer cuisineId);

    /**
     * Get all cuisines associated with a recipe
     * @param recipeId the recipe ID
     * @return list of cuisines
     */
    List<Cuisine> getRecipeCuisineList(Long recipeId);

    /**
     * Update cuisines for a recipe (remove existing associations and create new ones)
     * @param recipeId the recipe ID
     * @param cuisineIds set of cuisine IDs
     */
    void updateRecipeCuisines(Long recipeId, Set<Integer> cuisineIds);
}

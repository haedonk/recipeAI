package com.haekitchenapp.recipeapp.service;

import com.haekitchenapp.recipeapp.entity.Recipe;
import com.haekitchenapp.recipeapp.entity.RecipeStage;
import com.haekitchenapp.recipeapp.exception.RecipeNotFoundException;
import com.haekitchenapp.recipeapp.model.request.recipeStage.RecipeStageRequest;
import com.haekitchenapp.recipeapp.model.response.ApiResponse;
import com.haekitchenapp.recipeapp.repository.RecipeStageRepository;
import com.haekitchenapp.recipeapp.utility.RecipeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecipeStageService {

    private final RecipeStageRepository recipeRepository;

    private final RecipeMapper recipeMapper;

    /**
     * Finds a recipe by its ID.
     *
     * @param id the ID of the recipe to find
     * @return the found recipe
     * @throws RecipeNotFoundException if no recipe is found with the given ID
     * @throws IllegalArgumentException if the ID is null
     */
    public ResponseEntity<ApiResponse<RecipeStage>> findById(Long id) throws RecipeNotFoundException {
        if (id == null) {
            throw new IllegalArgumentException("RecipeStage ID must not be null");
        }
        RecipeStage recipe = findRecipeById(id);
        return ResponseEntity.ok(ApiResponse.success("RecipeStage retrieved successfully", recipe));
    }

    /**
     * Creates a new recipe.
     *
     * @param recipe the recipe to create
     * @return the created recipe
     * @throws IllegalArgumentException if the recipe ID is not null or if a data integrity violation occurs
     */
    public ResponseEntity<ApiResponse<RecipeStage>> create(RecipeStageRequest recipe) {
        if(recipe.getId() != null) recipe.setId(null); // Ensure ID is null for creation
        RecipeStage saved = saveRecipe(recipeMapper.toEntity(recipe));
        log.info("RecipeStage created successfully: {}", saved);
        return ResponseEntity.ok(ApiResponse.success("RecipeStage created successfully", saved));
    }

    /**
     * Saves a new recipe.
     *
     * @param recipe the recipe to save
     * @return the saved recipe
     * @throws IllegalArgumentException if the recipe data is invalid or if a data integrity violation occurs
     */
    private RecipeStage saveRecipe(RecipeStage recipe) {
        log.info("Saving recipe: {}", recipe);
        try {
            recipe = recipeRepository.save(recipe);
            log.info("RecipeStage saved successfully: {}", recipe);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while saving recipe: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid recipe data", e);
        }
        return recipe;
    }

    /**
     * Updates an existing recipe.
     *
     * @param recipe the recipe to update
     * @return the updated recipe
     * @throws IllegalArgumentException if the recipe ID is null or if no recipe is found with the given ID
     */
    public ResponseEntity<ApiResponse<RecipeStage>> update(RecipeStageRequest recipe) {
        if(recipe.getId() == null) throw new IllegalArgumentException("RecipeStage ID must not be null for update");
        RecipeStage existingRecipe = recipeRepository.findById(recipe.getId())
                .orElseThrow(() -> new IllegalArgumentException("RecipeStage not found with ID: " + recipe.getId()));
        RecipeStage saved = updateRecipe(recipeMapper.toEntity(existingRecipe, recipe));
        log.info("RecipeStage updated successfully: {}", saved);
        return ResponseEntity.ok(ApiResponse.success("RecipeStage updated successfully", saved));
    }

    /**
     * Updates an existing recipe.
     *
     * @param recipe the recipe to update
     * @return the updated recipe
     * @throws IllegalArgumentException if the recipe data is invalid or if a data integrity violation occurs
     */
    public RecipeStage updateRecipe(RecipeStage recipe) {
        log.info("Updating recipe: {}", recipe);
        try {
            recipe = recipeRepository.save(recipe);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while updating recipe: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid recipe data", e);
        }
        return recipe;
    }

    /**
     * Deletes a recipe by its ID.
     *
     * @param id the ID of the recipe to delete
     * @throws IllegalArgumentException if no recipe is found with the given ID
     */

    public ResponseEntity<ApiResponse<Object>> deleteById(Long id) {
        deleteRecipeById(id);
        return ResponseEntity.ok(ApiResponse.success("RecipeStage deleted successfully"));
    }
    public void deleteRecipeById(Long id) {
        log.info("Deleting recipe by ID: {}", id);
        RecipeStage recipe = recipeRepository.findByIdWithIngredients(id)
                .orElseThrow(() -> new RecipeNotFoundException("RecipeStage not found with ID: " + id));
        log.info("Found recipe to delete: {}", recipe);
        recipeRepository.delete(recipe); // Triggers cascade delete
    }

    public RecipeStage findRecipeById(Long id) {
        log.info("Finding recipe by ID: {}", id);
        if (id == null) {
            throw new IllegalArgumentException("RecipeStage ID must not be null");
        }
        return recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException("RecipeStage not found with ID: " + id));
    }

    public RecipeStage getRecipeByIdWithIngredients(Long id) {
    log.info("Fetching recipe by ID with ingredients: {}", id);
        if (id == null) {
            throw new IllegalArgumentException("RecipeStage ID must not be null");
        }
        return recipeRepository.findByIdWithIngredients(id)
                .orElseThrow(() -> new RecipeNotFoundException("RecipeStage not found with ID: " + id));
    }

    public void deleteAndFlush(Long recipeId) {
    log.info("Deleting and flushing recipe by ID: {}", recipeId);
        if (recipeId == null) {
            throw new IllegalArgumentException("RecipeStage ID must not be null");
        }
        RecipeStage recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RecipeNotFoundException("RecipeStage not found with ID: " + recipeId));
        recipeRepository.delete(recipe);
        recipeRepository.flush();
        log.info("RecipeStage deleted and flushed successfully for ID: {}", recipeId);
    }
}

package com.haekitchenapp.recipeapp.service;

import com.haekitchenapp.recipeapp.entity.Recipe;
import com.haekitchenapp.recipeapp.exception.RecipeNotFoundException;
import com.haekitchenapp.recipeapp.exception.RecipeSearchFoundNoneException;
import com.haekitchenapp.recipeapp.model.request.RecipeRequest;
import com.haekitchenapp.recipeapp.model.response.ApiResponse;
import com.haekitchenapp.recipeapp.model.response.RecipeBulkResponse;
import com.haekitchenapp.recipeapp.model.response.RecipeResponse;
import com.haekitchenapp.recipeapp.model.response.RecipeTitleDto;
import com.haekitchenapp.recipeapp.repository.IngredientRepository;
import com.haekitchenapp.recipeapp.repository.RecipeRepository;
import com.haekitchenapp.recipeapp.utility.RecipeMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecipeService {

    @Autowired
    private final RecipeRepository recipeRepository;

    @Autowired
    private final IngredientRepository ingredientRepository;

    @Autowired
    private final RecipeMapper recipeMapper;

    public ResponseEntity<ApiResponse<List<Recipe>>> findAll() throws RecipeNotFoundException {
        log.info("Fetching all recipes");
        List<Recipe> recipes = recipeRepository.findAll();
        if (recipes.isEmpty()) {
            log.warn("No recipes found");
            throw new RecipeNotFoundException("No recipes found");
        }
        log.info("Found {} recipes", recipes.size());
        return ResponseEntity.ok(ApiResponse.success("Recipes retrieved successfully", recipes));
    }

    public ResponseEntity<ApiResponse<List<RecipeTitleDto>>> searchByTitle(String title) throws RecipeSearchFoundNoneException {
        List<RecipeTitleDto> recipes = search(title);
        log.info("Recipes found: {}", recipes.size());
        if (recipes.isEmpty()) {
            throw new RecipeSearchFoundNoneException("No recipes found with title: " + title);
        }
        return ResponseEntity.ok(ApiResponse.success("Recipes retrieved successfully", recipes));
    }

    public ResponseEntity<ApiResponse<RecipeBulkResponse>> getNumberOfRecipes(int page) throws RecipeNotFoundException {
        log.info("Fetching all recipes with ingredients for page {}", page);
        PageRequest pageable = PageRequest.of(page, 1000);
        Page<Recipe> recipePage = recipeRepository.findAllWithIngredients(pageable);

        if (recipePage.isEmpty()) {
            log.warn("No recipes found with ingredients");
            throw new RecipeNotFoundException("No more recipes found with ingredients");
        }

        List<RecipeResponse> recipeResponses = recipePage.getContent().stream()
                .map(recipeMapper::toRecipeResponse)
                .toList();

        RecipeBulkResponse bulkResponse = new RecipeBulkResponse(recipeResponses, recipePage.isLast());

        log.info("Found and mapped {} recipes with ingredients", recipePage.getContent().size());
        return ResponseEntity.ok(ApiResponse.success("Recipes with ingredients retrieved successfully", bulkResponse));
    }

    public List<RecipeTitleDto> search(String title) throws RecipeSearchFoundNoneException {
        log.info("Searching recipe titles by title: {}", title);
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be null or empty");
        }
        PageRequest pageRequest = PageRequest.of(0, 20);
        List<RecipeTitleDto> recipes = recipeRepository.findTitlesByTitleContainingIgnoreCase(title, pageRequest);
        if(recipes.isEmpty()) {
            log.warn("No recipes found with title: {}", title);
            throw new RecipeSearchFoundNoneException("No recipes found with title: " + title);
        }
        log.info("Found {} recipe titles matching: {}", recipes.size(), title);
        return recipes;
    }

    public ResponseEntity<ApiResponse<Recipe>> findById(Long id) throws RecipeNotFoundException {
        log.info("Finding recipe by ID: {}", id);
        if (id == null) {
            throw new IllegalArgumentException("Recipe ID must not be null");
        }
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RecipeNotFoundException("Recipe not found with ID: " + id));
        return ResponseEntity.ok(ApiResponse.success("Recipe retrieved successfully", recipe));
    }

    public ResponseEntity<ApiResponse<List<Recipe>>> createBulk(List<@Valid RecipeRequest> recipes) {
        log.info("Creating {} recipes in bulk", recipes.size());
        if (recipes.isEmpty()) {
            log.warn("No recipes provided for creation");
            return ResponseEntity.ok(ApiResponse.success("No recipes to create"));
        }
        List<Recipe> savedRecipes = recipes.stream()
                .map(recipeMapper::toEntity)
                .map(this::saveRecipe)
                .toList();
        log.info("{} recipes created successfully", savedRecipes.size());
        return ResponseEntity.ok(ApiResponse.success("Recipes created successfully", savedRecipes));
    }

    public ResponseEntity<ApiResponse<Recipe>> create(RecipeRequest recipe) {
        if(recipe.getId() != null) recipe.setId(null); // Ensure ID is null for creation
        Recipe saved = saveRecipe(recipeMapper.toEntity(recipe));
        log.info("Recipe created successfully: {}", saved);
        return ResponseEntity.ok(ApiResponse.success("Recipe created successfully", saved));
    }

    private Recipe saveRecipe(Recipe recipe) {
        log.info("Saving recipe: {}", recipe);
        try {
            recipe = recipeRepository.save(recipe);
            log.info("Recipe saved successfully: {}", recipe);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while saving recipe: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid recipe data", e);
        }
        return recipe;
    }

    public ResponseEntity<ApiResponse<Recipe>> update(RecipeRequest recipe) {
        if(recipe.getId() == null) throw new IllegalArgumentException("Recipe ID must not be null for update");
        Recipe existingRecipe = recipeRepository.findById(recipe.getId())
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found with ID: " + recipe.getId()));
        Recipe saved = updateRecipe(recipeMapper.toEntity(existingRecipe, recipe));
        log.info("Recipe updated successfully: {}", saved);
        return ResponseEntity.ok(ApiResponse.success("Recipe created successfully", saved));
    }

    private Recipe updateRecipe(Recipe recipe) {
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
     * @return
     * @throws IllegalArgumentException if no recipe is found with the given ID
     */

    public ResponseEntity<ApiResponse<Object>> deleteById(Long id) {
        log.info("Deleting recipe by ID: {}", id);
        if (!recipeRepository.existsById(id)) {
            throw new IllegalArgumentException("Recipe not found with ID: " + id);
        }
        recipeRepository.deleteById(id);
        log.info("Recipe deleted successfully with ID: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Recipe deleted successfully"));
    }


    /**
     * Deletes all recipes from the repository.
     * This method is used to clear the recipe database.
     * @return a ResponseEntity containing an ApiResponse indicating success
     */
    public ResponseEntity<ApiResponse<Object>> deleteAll() {
        log.info("Deleting all recipes");
        deleteAllRecipes();
        log.info("All recipes deleted successfully");
        return ResponseEntity.ok(ApiResponse.success("All recipes deleted successfully"));
    }

    public void deleteAllRecipes() {
        List<Recipe> recipes = recipeRepository.findAll();
        for (Recipe recipe : recipes) {
            recipe.getIngredients().clear(); // clear child references
        }
        recipeRepository.deleteAll(); // now safe to delete
    }
}

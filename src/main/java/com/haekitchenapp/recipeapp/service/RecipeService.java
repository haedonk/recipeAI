package com.haekitchenapp.recipeapp.service;

import com.haekitchenapp.recipeapp.entity.Recipe;
import com.haekitchenapp.recipeapp.exception.RecipeNotFoundException;
import com.haekitchenapp.recipeapp.exception.RecipeSearchFoundNoneException;
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeRequest;
import com.haekitchenapp.recipeapp.model.response.*;
import com.haekitchenapp.recipeapp.model.response.batch.Status;
import com.haekitchenapp.recipeapp.model.response.recipe.*;
import com.haekitchenapp.recipeapp.repository.IngredientRepository;
import com.haekitchenapp.recipeapp.repository.RecipeRepository;
import com.haekitchenapp.recipeapp.utility.RecipeMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;

    private final RecipeMapper recipeMapper;

    /**
     * Fetches all recipes from the repository.
     *
     * @return a response entity containing a list of all recipes
     * @throws RecipeNotFoundException if no recipes are found
     */
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

    public ResponseEntity<ApiResponse<RecipeDuplicatesByTitleResponse>> findDuplicateTitles(int page) {
        log.info("Finding duplicate recipe titles for page {}", page);
        PageRequest pageable = PageRequest.of(page, 20);
        Page<RecipeDuplicatesByTitleDto> duplicatesPage = recipeRepository.findDuplicateTitles(pageable);

        if (duplicatesPage.isEmpty()) {
            log.warn("No duplicate titles found");
            return ResponseEntity.ok(ApiResponse.success("No more duplicate titles found"));
        }

        RecipeDuplicatesByTitleResponse recipeDuplicatesByTitleResponse = new RecipeDuplicatesByTitleResponse(
                duplicatesPage.getContent(),
                duplicatesPage.isLast()
        );

        log.info("Found {} duplicate titles", recipeDuplicatesByTitleResponse.getDuplicates().size());
        return ResponseEntity.ok(ApiResponse.success("Duplicate titles retrieved successfully", recipeDuplicatesByTitleResponse));
    }

    /**
     * Searches for recipes by title and returns a paginated response.
     *
     * @param title the title to search for
     * @return a response entity containing a list of recipe titles matching the search criteria
     * @throws RecipeSearchFoundNoneException if no recipes are found with the given title
     */
    public ResponseEntity<ApiResponse<List<RecipeTitleDto>>> searchByTitle(String title) throws RecipeSearchFoundNoneException {
        List<RecipeTitleDto> recipes = search(title);
        log.info("Recipes found: {}", recipes.size());
        if (recipes.isEmpty()) {
            throw new RecipeSearchFoundNoneException("No recipes found with title: " + title);
        }
        return ResponseEntity.ok(ApiResponse.success("Recipes retrieved successfully", recipes));
    }

    /**
     * Fetches all recipes with their ingredients for a given page.
     *
     * @param page the page number to fetch
     * @return a response entity containing a bulk response of recipes with ingredients
     * @throws RecipeNotFoundException if no recipes are found for the given page
     */
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

/**
     * Searches for recipe titles containing the specified title.
     *
     * @param title the title to search for
     * @return a list of recipe titles matching the search criteria
     * @throws RecipeSearchFoundNoneException if no recipes are found with the given title
     * @throws IllegalArgumentException if the title is null or empty
     */
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

    public ResponseEntity<ApiResponse<List<RecipeTitleDto>>> findAllIdsWithTitle(String title) throws RecipeNotFoundException {
        log.info("Finding all recipe with title: {}", title);
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title must not be null or empty");
        }
        List<RecipeTitleDto> recipes = recipeRepository.findIdsByTitle(title);
        if (recipes.isEmpty()) {
            log.warn("No recipes ids found with title: {}", title);
            throw new RecipeNotFoundException("No recipes ids found with title: " + title);
        }
        log.info("Found {} recipe ids with title: {}", recipes.size(), title);
        return ResponseEntity.ok(ApiResponse.success("Recipes ids successfully", recipes));
    }

    /**
     * Finds recipe details by ID, including ingredients.
     *
     * @param id the ID of the recipe to find
     * @return the recipe details
     * @throws RecipeNotFoundException if no recipe is found with the given ID
     * @throws IllegalArgumentException if the ID is null
     */
    public ResponseEntity<ApiResponse<RecipeDetailsDto>> getRecipeDetails(Long id) throws RecipeNotFoundException {
        log.info("Finding recipe details by ID: {}", id);
        if (id == null) {
            throw new IllegalArgumentException("Recipe ID must not be null");
        }

        Recipe recipeDetails = recipeRepository.findByIdWithIngredients(id)
                .orElseThrow(() -> new RecipeNotFoundException("Recipe details not found with ID: " + id));

        RecipeDetailsDto recipeDetailsDto = recipeMapper.toLlmDetailsDto(recipeDetails);

        return ResponseEntity.ok(ApiResponse.success("Recipe details retrieved successfully", recipeDetailsDto));
    }

    /**
     * Finds a recipe by its ID.
     *
     * @param id the ID of the recipe to find
     * @return the found recipe
     * @throws RecipeNotFoundException if no recipe is found with the given ID
     * @throws IllegalArgumentException if the ID is null
     */
    public ResponseEntity<ApiResponse<Recipe>> findById(Long id) throws RecipeNotFoundException {
        if (id == null) {
            throw new IllegalArgumentException("Recipe ID must not be null");
        }
        Recipe recipe = findRecipeById(id);
        return ResponseEntity.ok(ApiResponse.success("Recipe retrieved successfully", recipe));
    }

    public ResponseEntity<ApiResponse<List<RecipeTitleDto>>> findRecipeByCreatedBy(Long userId){
        log.info("Finding recipes created by user ID: {}", userId);
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        List<RecipeTitleDto> recipes = recipeRepository.findTitlesByCreatedBy(userId);
        if (recipes.isEmpty()) {
            log.warn("No recipes found for user ID: {}", userId);
            throw new RecipeNotFoundException("No recipes found for user ID: " + userId);
        }
        log.info("Found {} recipes created by user ID: {}", recipes.size(), userId);
        return ResponseEntity.ok(ApiResponse.success("Recipes retrieved successfully", recipes));
    }

    /**
     * Creates a bulk of recipes.
     *
     * @param recipes the list of recipes to create
     * @return the list of created recipes
     * @throws IllegalArgumentException if any recipe ID is not null or if a data integrity violation occurs
     */
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

    /**
     * Creates a new recipe.
     *
     * @param recipe the recipe to create
     * @return the created recipe
     * @throws IllegalArgumentException if the recipe ID is not null or if a data integrity violation occurs
     */
    public ResponseEntity<ApiResponse<Recipe>> create(RecipeRequest recipe) {
        if(recipe.getId() != null) recipe.setId(null); // Ensure ID is null for creation
        Recipe saved = saveRecipe(recipeMapper.toEntity(recipe));
        log.info("Recipe created successfully: {}", saved);
        return ResponseEntity.ok(ApiResponse.success("Recipe created successfully", saved));
    }

    /**
     * Saves a new recipe.
     *
     * @param recipe the recipe to save
     * @return the saved recipe
     * @throws IllegalArgumentException if the recipe data is invalid or if a data integrity violation occurs
     */
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

    /**
     * Updates an existing recipe.
     *
     * @param recipe the recipe to update
     * @return the updated recipe
     * @throws IllegalArgumentException if the recipe ID is null or if no recipe is found with the given ID
     */
    public ResponseEntity<ApiResponse<Recipe>> update(RecipeRequest recipe) {
        if(recipe.getId() == null) throw new IllegalArgumentException("Recipe ID must not be null for update");
        Recipe existingRecipe = recipeRepository.findById(recipe.getId())
                .orElseThrow(() -> new IllegalArgumentException("Recipe not found with ID: " + recipe.getId()));
        Recipe saved = updateRecipe(recipeMapper.toEntity(existingRecipe, recipe));
        log.info("Recipe updated successfully: {}", saved);
        return ResponseEntity.ok(ApiResponse.success("Recipe updated successfully", saved));
    }

    /**
     * Updates an existing recipe.
     *
     * @param recipe the recipe to update
     * @return the updated recipe
     * @throws IllegalArgumentException if the recipe data is invalid or if a data integrity violation occurs
     */
    public Recipe updateRecipe(Recipe recipe) {
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
        return ResponseEntity.ok(ApiResponse.success("Recipe deleted successfully"));
    }


    @Transactional
    public ResponseEntity<ApiResponse<Object>> deleteRecipesByIds(List<Long> ids) {
        log.info("Deleting all recipes");
        if (ids == null || ids.isEmpty()) {
            log.warn("No recipe IDs provided for deletion");
            return ResponseEntity.ok(ApiResponse.success("No recipes to delete"));
        }
        for (Long id : ids) {
            if (id != null) {
                try {
                    deleteRecipeById(id);
                } catch (Exception e) {
                    log.error("Error deleting recipe with ID {}: {}", id, e.getMessage());
                }
            }
        }
        log.info("All recipes deleted successfully");
        return ResponseEntity.ok(ApiResponse.success("All recipes deleted successfully"));
    }

    private void deleteRecipeById(Long id) {
        log.info("Deleting recipe by ID: {}", id);
        recipeRepository.deleteById(id);
        log.info("Recipe deleted successfully with ID: {}", id);
    }

    public Recipe findRecipeById(Long id) {
        log.info("Finding recipe by ID: {}", id);
        if (id == null) {
            throw new IllegalArgumentException("Recipe ID must not be null");
        }
        return recipeRepository.findById(id)
                .orElse(null);
    }

    public RecipeTitleDto findRecipeTitleDtoById(Long id) {
        log.info("Finding recipe title dto by ID: {}", id);
        if (id == null) {
            throw new IllegalArgumentException("Recipe ID must not be null");
        }
        return recipeRepository.findRecipeTitleDtoById(id)
                .orElseThrow(() -> new RecipeNotFoundException("Recipe title dto not found with ID: " + id));
    }

    public Recipe getRecipeByIdWithIngredients(Long id) {
    log.info("Fetching recipe by ID with ingredients: {}", id);
        if (id == null) {
            throw new IllegalArgumentException("Recipe ID must not be null");
        }
        return recipeRepository.findByIdWithIngredients(id)
                .orElseThrow(() -> new RecipeNotFoundException("Recipe not found with ID: " + id));
    }

    public Status getRecipeMassageDetails() {
    log.info("Fetching recipe massage details");
        long totalRecipes = recipeRepository.count();
        Long recipesWithEmbedding = recipeRepository.countByEmbeddingIsNotNull();
        Status status = new Status();
        status.setTotal(totalRecipes);
        status.setCompleted(recipesWithEmbedding);
        status.setPercentage(
                totalRecipes == 0 ? 0f : (float) recipesWithEmbedding / totalRecipes * 100
        );
        return status;
    }
}

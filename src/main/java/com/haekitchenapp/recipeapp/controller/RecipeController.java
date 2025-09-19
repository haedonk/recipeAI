package com.haekitchenapp.recipeapp.controller;

import com.haekitchenapp.recipeapp.entity.Recipe;
import com.haekitchenapp.recipeapp.entity.Unit;
import com.haekitchenapp.recipeapp.exception.RecipeNotFoundException;
import com.haekitchenapp.recipeapp.exception.RecipeSearchFoundNoneException;
import com.haekitchenapp.recipeapp.model.request.recipe.EmbedUpdateRequest;
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeSimilarityRequest;
import com.haekitchenapp.recipeapp.model.response.*;
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeRequest;
import com.haekitchenapp.recipeapp.model.response.recipe.*;
import com.haekitchenapp.recipeapp.service.RecipeAIService;
import com.haekitchenapp.recipeapp.service.RecipeService;
import com.haekitchenapp.recipeapp.service.UnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@Validated
@RequestMapping("/api/recipes")
@Slf4j
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    private final UnitService unitService;

    private final RecipeAIService recipeAIService;

    // Create endpoints
    @PostMapping
    public ResponseEntity<ApiResponse<Recipe>> createRecipe(@RequestBody @Valid RecipeRequest recipeRequest) {
        log.info("Received request to create recipe: {}", recipeRequest);
        return recipeService.create(recipeRequest);
    }

    @GetMapping("/units")
    public ResponseEntity<ApiResponse<List<Unit>>> getUnits() {
        log.info("Received request to get all ingredients");
        return unitService.getAllUnits();
    }

    // Bulk create endpoint
    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<Recipe>>> createRecipes(@RequestBody @Valid List<RecipeRequest> recipeRequests) {
        log.info("Received request to create multiple recipes: {}", recipeRequests);
        return recipeService.createBulk(recipeRequests);
    }

    // Update endpoints
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Recipe>> updateRecipe(@PathVariable Long id, @RequestBody @Valid RecipeRequest recipeRequest) {
        log.info("Received request to update recipe with ID {}: {}", id, recipeRequest);
        recipeRequest.setId(id);
        return recipeService.update(recipeRequest);
    }

    // Update endpoints
    @PutMapping("/embed")
    public ResponseEntity<ApiResponse<Object>> updateRecipe(@RequestBody @Valid EmbedUpdateRequest recipeRequest) {
        log.info("Received request to update recipe embedding with ID {}: {}", recipeRequest.getId(), recipeRequest);
        return recipeService.updateEmbeddingOnly(recipeRequest);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecipeResponse>> getRecipeById(@PathVariable Long id) throws RecipeNotFoundException {
        log.info("Received request to get recipe by ID: {}", id);
        return recipeService.findById(id);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<RecipeTitleDto>>> searchRecipesByTitle(@RequestParam String title) throws RecipeSearchFoundNoneException {
        log.info("Received request to search recipes by title: {}", title);
        return recipeService.searchByTitle(title);
    }

    @GetMapping("/findByTitle/{title}")
    public ResponseEntity<ApiResponse<List<RecipeTitleDto>>> findRecipeIdsByTitle(@PathVariable String title) throws RecipeNotFoundException {
        log.info("Received request to find all recipes by title: {}", title);
        return recipeService.findAllIdsWithTitle(title);
    }

    @GetMapping("/findByTitle")
    public ResponseEntity<ApiResponse<List<RecipeTitleDto>>> findRecipeIdsByTitleByHeader(@RequestHeader("title") String title) throws RecipeNotFoundException {
    log.info("Received request to find all recipes by title from header: {}", title);
        return recipeService.findAllIdsWithTitle(title);
    }

    @GetMapping("/findByCreatedBy/{userId}")
    public ResponseEntity<ApiResponse<List<RecipeTitleDto>>> findRecipesByCreatedBy(@PathVariable Long userId) throws RecipeNotFoundException {
        log.info("Received request to find recipes created by user ID: {}", userId);
        return recipeService.findRecipeByCreatedBy(userId);
    }


    @GetMapping("/llm-details/{id}")
    public ResponseEntity<ApiResponse<RecipeDetailsDto>> getRecipeDetails(@PathVariable Long id) throws RecipeNotFoundException,
            ExecutionException, InterruptedException {
        log.info("Received request to get recipe details for ID: {}", id);
        return recipeService.getRecipeDetailsResponse(id);
    }

    @GetMapping("/duplicates/{page}")
    public ResponseEntity<ApiResponse<RecipeDuplicatesByTitleResponse>> getDuplicateRecipes(@PathVariable int page) {
        log.info("Received request to get duplicate recipes");
        return recipeService.findDuplicateTitles(page);
    }

    @PostMapping("/deleteList")
    public ResponseEntity<ApiResponse<Object>> deleteRecipesByIds(@RequestBody List<Long> ids) {
        log.info("Received request to delete recipes with IDs: {}", ids);
        return recipeService.deleteRecipesByIds(ids);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteRecipe(@PathVariable Long id) {
        log.info("Received request to delete recipe with ID: {}", id);
        return recipeService.deleteById(id);
    }
}

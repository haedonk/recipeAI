package com.haekitchenapp.recipeapp.controller;

import com.haekitchenapp.recipeapp.entity.Recipe;
import com.haekitchenapp.recipeapp.entity.RecipeStage;
import com.haekitchenapp.recipeapp.exception.RecipeNotFoundException;
import com.haekitchenapp.recipeapp.exception.RecipeSearchFoundNoneException;
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeSimilarityRequest;
import com.haekitchenapp.recipeapp.model.request.recipeStage.RecipeStageRequest;
import com.haekitchenapp.recipeapp.model.response.*;
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeRequest;
import com.haekitchenapp.recipeapp.model.response.recipe.*;
import com.haekitchenapp.recipeapp.service.RecipeService;
import com.haekitchenapp.recipeapp.service.RecipeStageService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/recipes")
@Slf4j
public class RecipeController {

    @Autowired
    private RecipeService recipeService;

    @Autowired
    RecipeStageService recipeStageService;

    // Create endpoints
    @PostMapping
    public ResponseEntity<ApiResponse<RecipeStage>> createRecipe(@RequestBody @Valid RecipeStageRequest recipeRequest) {
        log.info("Received request to create recipe: {}", recipeRequest);
        return recipeStageService.create(recipeRequest);
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


    // Get endpoints
    @GetMapping("/numberOfRecipes/{page}")
    public ResponseEntity<ApiResponse<RecipeBulkResponse>> getNumberOfRecipes(@PathVariable int page) throws RecipeNotFoundException {
        log.info("Received request to get the number of recipes");
        return recipeService.getNumberOfRecipes(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Recipe>> getRecipeById(@PathVariable Long id) throws RecipeNotFoundException {
        log.info("Received request to get recipe by ID: {}", id);
        return recipeService.findById(id);
    }

    @GetMapping("/stage/{id}")
    public ResponseEntity<ApiResponse<RecipeStage>> getRecipeStageById(@PathVariable Long id) throws RecipeNotFoundException {
        log.info("Received request to get recipe stage by ID: {}", id);
        return recipeStageService.findById(id);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<RecipeTitleDto>>> searchRecipesByTitle(@RequestParam String title) throws RecipeSearchFoundNoneException {
        log.info("Received request to search recipes by title: {}", title);
        return recipeService.searchByTitle(title);
    }

    @PostMapping("/searchSimilarity")
    public ResponseEntity<ApiResponse<List<RecipeSimilarityDto>>> searchRecipesByTitleSimilarity(@RequestBody @Valid RecipeSimilarityRequest query) throws RecipeSearchFoundNoneException {
        log.info("Received request to search recipes by query similarity: {}", query);
        return recipeService.searchByAdvancedEmbedding(query);
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
    public ResponseEntity<ApiResponse<RecipeDetailsDto>> getRecipeDetails(@PathVariable Long id) throws RecipeNotFoundException {
        log.info("Received request to get recipe details for ID: {}", id);
        return recipeService.getRecipeDetails(id);
    }

    @GetMapping("/duplicates/{page}")
    public ResponseEntity<ApiResponse<RecipeDuplicatesByTitleResponse>> getDuplicateRecipes(@PathVariable int page) {
        log.info("Received request to get duplicate recipes");
        return recipeService.findDuplicateTitles(page);
    }

    // removing the all endpoint for now, its resource intensive and not needed for the current use case.
//    @GetMapping("/all")
//    public ResponseEntity<ApiResponse<List<Recipe>>> getAllRecipes() throws RecipeNotFoundException {
//        log.info("Received request to get all recipes");
//        return recipeService.findAll();
//    }



    // Delete endpoints
    // need to update to take a list.  Delete all is not a good idea, but for testing purposes it is ok.
//    @DeleteMapping("/deleteAll")
//    public ResponseEntity<ApiResponse<Object>> deleteAllRecipes() {
//        log.info("Received request to delete all recipes");
//        return recipeService.deleteAll();
//    }

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

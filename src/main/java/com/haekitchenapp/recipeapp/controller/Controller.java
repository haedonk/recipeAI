package com.haekitchenapp.recipeapp.controller;

import com.haekitchenapp.recipeapp.entity.Recipe;
import com.haekitchenapp.recipeapp.exception.RecipeNotFoundException;
import com.haekitchenapp.recipeapp.exception.RecipeSearchFoundNoneException;
import com.haekitchenapp.recipeapp.model.request.RecipeRequest;
import com.haekitchenapp.recipeapp.model.response.ApiResponse;
import com.haekitchenapp.recipeapp.model.response.RecipeTitleDto;
import com.haekitchenapp.recipeapp.service.RecipeService;
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
public class Controller {

    @Autowired
    private RecipeService recipeService;

    // Create endpoints
    @PostMapping
    public ResponseEntity<ApiResponse<Recipe>> createRecipe(@RequestBody @Valid RecipeRequest recipeRequest) {
        log.info("Received request to create recipe: {}", recipeRequest);
        return recipeService.create(recipeRequest);
    }

    // Update endpoints
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Recipe>> updateRecipe(@PathVariable Long id, @RequestBody @Valid RecipeRequest recipeRequest) {
        log.info("Received request to update recipe with ID {}: {}", id, recipeRequest);
        recipeRequest.setId(id);
        return recipeService.update(recipeRequest);
    }


    // Get endpoints
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Recipe>> getRecipeById(@PathVariable Long id) throws RecipeNotFoundException {
        log.info("Received request to get recipe by ID: {}", id);
        return recipeService.findById(id);
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<RecipeTitleDto>>> searchRecipesByTitle(@RequestParam String title) throws RecipeSearchFoundNoneException {
        log.info("Received request to search recipes by title: {}", title);
        return recipeService.searchByTitle(title);
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<Recipe>>> getAllRecipes() throws RecipeNotFoundException {
        log.info("Received request to get all recipes");
        return recipeService.findAll();
    }



    // Delete endpoints
    @DeleteMapping("/deleteAll")
    public ResponseEntity<ApiResponse<Object>> deleteAllRecipes() {
        log.info("Received request to delete all recipes");
        return recipeService.deleteAll();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> deleteRecipe(@PathVariable Long id) {
        log.info("Received request to delete recipe with ID: {}", id);
        return recipeService.deleteById(id);
    }
}

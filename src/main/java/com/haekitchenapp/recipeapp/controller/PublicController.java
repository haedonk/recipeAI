package com.haekitchenapp.recipeapp.controller;

import com.haekitchenapp.recipeapp.entity.Recipe;
import com.haekitchenapp.recipeapp.entity.RecipeStage;
import com.haekitchenapp.recipeapp.exception.RecipeNotFoundException;
import com.haekitchenapp.recipeapp.exception.RecipeSearchFoundNoneException;
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeRequest;
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeSimilarityRequest;
import com.haekitchenapp.recipeapp.model.request.recipeStage.RecipeStageRequest;
import com.haekitchenapp.recipeapp.model.response.ApiResponse;
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
@RequestMapping("/api/public/recipes")
@Slf4j
public class PublicController {

    @Autowired
    private RecipeService recipeService;

    @Autowired
    RecipeStageService recipeStageService;


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

}

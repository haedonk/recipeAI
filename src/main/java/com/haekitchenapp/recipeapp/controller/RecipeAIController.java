package com.haekitchenapp.recipeapp.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.haekitchenapp.recipeapp.exception.RecipeSearchFoundNoneException;
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeSimilarityRequest;
import com.haekitchenapp.recipeapp.model.response.ApiResponse;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeAISkeletonId;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeSimilarityDto;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto;
import com.haekitchenapp.recipeapp.service.JwtTokenService;
import com.haekitchenapp.recipeapp.service.OpenAiApi;
import com.haekitchenapp.recipeapp.service.RecipeAIService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recipe-ai")
@Slf4j
@RequiredArgsConstructor
public class RecipeAIController {

    private final RecipeAIService recipeAIService;
    private final OpenAiApi openAiApi;
    private final JwtTokenService jwtTokenService;

    @GetMapping("/titles/random")
    public ResponseEntity<ApiResponse<List<RecipeTitleDto>>> getRandomTitles(@RequestParam Integer count) {
        log.info("Request for random recipe titles");
        return recipeAIService.generateRandomRecipeTitles(count);
    }

    @PostMapping("/searchSimilarity/string")
    public ResponseEntity<ApiResponse<List<RecipeSimilarityDto>>> searchRecipesByTitleSimilarity(@RequestBody @Valid String query) throws RecipeSearchFoundNoneException {
        log.info("Received request to search recipes by query similarity: {}", query);
        return recipeAIService.searchByAdvancedEmbedding(query);
    }

    @PostMapping("/searchSimilarity/object")
    public ResponseEntity<ApiResponse<List<RecipeSimilarityDto>>> searchRecipesByTitleSimilarity(@RequestBody @Valid RecipeSimilarityRequest query) throws RecipeSearchFoundNoneException {
        log.info("Received request to search recipes by query similarity: {}", query);
        return recipeAIService.searchByAdvancedEmbeddingObject(query);
    }


    @PostMapping("/chat/recipe")
    public ResponseEntity<ApiResponse<Long>> recipeChat(@RequestBody @Valid String query, HttpServletRequest request) throws JsonProcessingException {
        Long userId = jwtTokenService.getUserIdFromRequest(request);
        log.info("Recipe chat request - User ID: {}", userId);
        return recipeAIService.recipeChat(query, userId);
    }

    @PostMapping("/chat/correct-recipe")
    public ResponseEntity<ApiResponse<Long>> correctRecipeChat(@RequestBody @Valid RecipeAISkeletonId query, HttpServletRequest request) throws JsonProcessingException {
        Long userId = jwtTokenService.getUserIdFromRequest(request);
        log.info("Correct recipe chat request - User ID: {}", userId);
        return recipeAIService.recipeCleanUp(query, userId);
    }
}

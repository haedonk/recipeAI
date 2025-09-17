package com.haekitchenapp.recipeapp.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.haekitchenapp.recipeapp.exception.RecipeSearchFoundNoneException;
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeSimilarityRequest;
import com.haekitchenapp.recipeapp.model.response.ApiResponse;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeAISkeleton;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeAISkeletonId;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeSimilarityDto;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto;
import com.haekitchenapp.recipeapp.service.OpenAiApi;
import com.haekitchenapp.recipeapp.service.RecipeAIService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recipe-ai")
@Slf4j
public class RecipeAIController {

    @Autowired
    private RecipeAIService recipeAIService;

    @Autowired
    private OpenAiApi openAiApi;

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
    public ResponseEntity<ApiResponse<Long>> recipeChat(@RequestBody @Valid String query) throws JsonProcessingException {
        // Get the user ID from the security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        log.info("Recipe chat request - User: {}", query);
        return recipeAIService.recipeChat(query, userName);
    }

    @PostMapping("/chat/correct-recipe")
    public ResponseEntity<ApiResponse<Long>> correctRecipeChat(@RequestBody @Valid RecipeAISkeletonId query) throws JsonProcessingException {
        // Get the user ID from the security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userName = authentication.getName();
        log.info("Correct recipe chat request - User: {}", userName);
        return recipeAIService.recipeCleanUp(query, userName);
    }

}

package com.haekitchenapp.recipeapp.controller;

import com.haekitchenapp.recipeapp.entity.Recipe;
import com.haekitchenapp.recipeapp.entity.RecipeLikes;
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeUserLikeDto;
import com.haekitchenapp.recipeapp.model.response.ApiResponse;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto;
import com.haekitchenapp.recipeapp.service.UserInteractionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/interaction")
@Slf4j
public class UserInteractionController {


    @Autowired
    private UserInteractionService userInteractionService;

    @PostMapping("/like-recipe")
    public ResponseEntity<ApiResponse<Boolean>> toggleRecipeLike(@RequestBody @Valid RecipeUserLikeDto recipeUserLikeDto) {
        log.info("Received request to toggle like for recipe ID: {} by user ID: {}", recipeUserLikeDto.getRecipeId(),
                recipeUserLikeDto.getUserId());
        return userInteractionService.toggleRecipeLike(recipeUserLikeDto.getRecipeId(), recipeUserLikeDto.getUserId());
    }

    @GetMapping("/like-recipe/{userId}")
    public ResponseEntity<ApiResponse<List<RecipeLikes>>> getRecipeLikesByUserId(
            @PathVariable Long userId) {
        log.info("Received request to get recipe likes for user ID: {}", userId);
        return userInteractionService.getRecipeLikesByUserId(userId);
    }

    @GetMapping("/like-recipe/recipes/{userId}")
    public ResponseEntity<ApiResponse<List<RecipeTitleDto>>> getRecipeLikesByUserIdForRecipes(
            @PathVariable Long userId) {
        log.info("Received request to get recipes likes for user ID: {}", userId);
        return userInteractionService.getRecipeTitleDtosByUserId(userId);
    }

    @GetMapping("/like-recipe/recipe/{recipeId}")
    public ResponseEntity<ApiResponse<List<RecipeLikes>>> getRecipeLikesByRecipeId(
            @PathVariable Long recipeId) {
        log.info("Received request to get recipe likes for recipe ID: {}", recipeId);
        return userInteractionService.getRecipeLikesByRecipeId(recipeId);
    }

    @GetMapping("/like-recipe/single/{userId}/{recipeId}")
    public ResponseEntity<ApiResponse<Boolean>> isRecipeLikedByUser(
            @PathVariable Long userId, @PathVariable Long recipeId) {
        log.info("Received request to check if recipe ID: {} is liked by user ID: {}", recipeId, userId);
        return userInteractionService.isRecipeLikedByUser(userId, recipeId);
    }

}

package com.haekitchenapp.recipeapp.controller;

import com.haekitchenapp.recipeapp.entity.RecipeLikes;
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeUserLikeDto;
import com.haekitchenapp.recipeapp.model.response.ApiResponse;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto;
import com.haekitchenapp.recipeapp.service.JwtTokenService;
import com.haekitchenapp.recipeapp.service.UserInteractionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/interaction")
@Slf4j
@RequiredArgsConstructor
public class UserInteractionController {

    private final UserInteractionService userInteractionService;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/like-recipe")
    public ResponseEntity<ApiResponse<Boolean>> toggleRecipeLike(@RequestBody @Valid RecipeUserLikeDto recipeUserLikeDto, HttpServletRequest request) {
        Long userId = jwtTokenService.getUserIdFromRequest(request);
        log.info("Received request to toggle like for recipe ID: {} by user ID: {}", recipeUserLikeDto.getRecipeId(), userId);
        return userInteractionService.toggleRecipeLike(recipeUserLikeDto.getRecipeId(), userId);
    }

    @GetMapping("/like-recipe")
    public ResponseEntity<ApiResponse<List<RecipeLikes>>> getRecipeLikesByUserId(HttpServletRequest request) {
        Long userId = jwtTokenService.getUserIdFromRequest(request);
        log.info("Received request to get recipe likes for user ID: {}", userId);
        return userInteractionService.getRecipeLikesByUserId(userId);
    }

    @GetMapping("/like-recipe/recipes")
    public ResponseEntity<ApiResponse<List<RecipeTitleDto>>> getRecipeLikesByUserIdForRecipes(HttpServletRequest request) {
        Long userId = jwtTokenService.getUserIdFromRequest(request);
        log.info("Received request to get recipes likes for user ID: {}", userId);
        return userInteractionService.getRecipeTitleDtosByUserId(userId);
    }

    @GetMapping("/like-recipe/recipe/{recipeId}")
    public ResponseEntity<ApiResponse<List<RecipeLikes>>> getRecipeLikesByRecipeId(
            @PathVariable Long recipeId) {
        log.info("Received request to get recipe likes for recipe ID: {}", recipeId);
        return userInteractionService.getRecipeLikesByRecipeId(recipeId);
    }

    @GetMapping("/like-recipe/single/{recipeId}")
    public ResponseEntity<ApiResponse<Boolean>> isRecipeLikedByUser(@PathVariable Long recipeId, HttpServletRequest request) {
        Long userId = jwtTokenService.getUserIdFromRequest(request);
        log.info("Received request to check if recipe ID: {} is liked by user ID: {}", recipeId, userId);
        return userInteractionService.isRecipeLikedByUser(userId, recipeId);
    }
}

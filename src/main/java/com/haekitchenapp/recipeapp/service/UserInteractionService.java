package com.haekitchenapp.recipeapp.service;

import com.haekitchenapp.recipeapp.entity.RecipeLikes;
import com.haekitchenapp.recipeapp.entity.composite.RecipeLikesId;
import com.haekitchenapp.recipeapp.model.response.ApiResponse;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto;
import com.haekitchenapp.recipeapp.repository.RecipeLikesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserInteractionService {

    private final RecipeLikesRepository recipeLikesRepository;

    private final RecipeService recipeService;

    public ResponseEntity<ApiResponse<Boolean>> toggleRecipeLike(Long recipeId, Long userId) {
        recipeService.findById(recipeId);

        log.info("Toggling like for recipe with ID {} by user with ID {}", recipeId, userId);

        RecipeLikes recipeLikes = new RecipeLikes();
        recipeLikes.setId(new RecipeLikesId(userId, recipeId));

        boolean isLiked = recipeLikesRepository.existsById(recipeLikes.getId());

        if (isLiked) {
            unlikeRecipe(recipeId, userId);
            return ResponseEntity.ok(ApiResponse.success("Recipe unliked successfully", true));
        } else {
            likeRecipe(recipeId, userId);
            return ResponseEntity.ok(ApiResponse.success("Recipe liked successfully", true));
        }
    }


    public ResponseEntity<ApiResponse<List<RecipeLikes>>> getRecipeLikesByUserId(Long userId) {
        log.info("Fetching recipe likes for user with ID {}", userId);
        return ResponseEntity.ok(ApiResponse.success("Recipe likes fetched successfully", recipeLikesRepository.findByIdUserId(userId)));
    }

    public ResponseEntity<ApiResponse<List<RecipeLikes>>> getRecipeLikesByRecipeId(Long recipeId) {
        log.info("Fetching recipe likes for recipe with ID {}", recipeId);
        recipeService.findById(recipeId);
        return ResponseEntity.ok(ApiResponse.success("Recipe likes fetched successfully", recipeLikesRepository.findByIdRecipeId(recipeId)));
    }

    public ResponseEntity<ApiResponse<List<RecipeTitleDto>>> getRecipeTitleDtosByUserId(Long userId) {
        log.info("Fetching recipes for user with ID {}", userId);
        List<RecipeLikes> recipeLikesList = recipeLikesRepository.findByIdUserId(userId);

        if (recipeLikesList.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No recipes found for this user", null));
        }

        List<RecipeTitleDto> recipes = recipeLikesList.stream()
                .map(like -> recipeService.findRecipeTitleDtoById(like.getId().getRecipeId()))
                .filter(Objects::nonNull)
                .toList();

        if (recipes.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No recipes found for this user", null));
        }

        return ResponseEntity.ok(ApiResponse.success("Recipes fetched successfully", recipes));
    }


    private void unlikeRecipe(Long recipeId, Long userId) {
        log.info("User with ID {} unliked recipe with ID {}", userId, recipeId);
        RecipeLikesId recipeLikesId = new RecipeLikesId(userId, recipeId);
        recipeLikesRepository.deleteById(recipeLikesId);
    }

    private void likeRecipe(Long recipeId, Long userId) {
        log.info("User with ID {} liked recipe with ID {}", userId, recipeId);
        RecipeLikesId recipeLikesId = new RecipeLikesId(userId, recipeId);
        RecipeLikes recipeLikes = new RecipeLikes();
        recipeLikes.setId(recipeLikesId);
        recipeLikesRepository.save(recipeLikes);
    }

    public ResponseEntity<ApiResponse<Boolean>> isRecipeLikedByUser(Long userId, Long recipeId) {
        log.info("Checking if recipe with ID {} is liked by user with ID {}", recipeId, userId);
        boolean isLiked = recipeLikesRepository.existsById(new RecipeLikesId(userId, recipeId));
        log.info("Recipe with ID {} liked by user with ID {}: {}", recipeId, userId, isLiked);
        return ResponseEntity.ok(ApiResponse.success("Recipe like status fetched successfully", isLiked));
    }
}

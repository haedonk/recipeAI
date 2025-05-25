package com.haekitchenapp.recipeapp.repository;

import com.haekitchenapp.recipeapp.entity.RecipeIngredient;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {

    @NonNull
    Optional<RecipeIngredient> findById(@NonNull Long id);

    Optional<RecipeIngredient> findByIdAndRecipeId(Long id, Long recipeId);

    void deleteByIdAndRecipeId(Long id, Long recipeId);

    boolean existsByIdAndRecipeId(Long id, Long recipeId);

    Optional<RecipeIngredient> findByIngredientNameAndRecipeId(String ingredientName, Long recipeId);

}

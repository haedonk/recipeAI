package com.haekitchenapp.recipeapp.repository;

import com.haekitchenapp.recipeapp.entity.RecipeIngredient;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeIngredientProjection;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {

    @NonNull
    Optional<RecipeIngredient> findById(@NonNull Long id);

    List<RecipeIngredient> findByRecipeId(@NonNull Long recipeId);

    @Query("SELECT ri.ingredient.id FROM RecipeIngredient ri WHERE ri.recipe.id = :recipeId")
    List<Long> findIngredientIdsByRecipeId(@NonNull Long recipeId);
}

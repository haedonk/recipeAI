package com.haekitchenapp.recipeapp.repository;

import com.haekitchenapp.recipeapp.entity.RecipeIngredient;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {

    @NonNull
    Optional<RecipeIngredient> findById(@NonNull Long id);

    List<RecipeIngredient> findByRecipeId(@NonNull Long recipeId);
}

package com.haekitchenapp.recipeapp.repository;

import com.haekitchenapp.recipeapp.entity.RecipeUpdateFailure;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeUpdateFailureRepository extends JpaRepository<RecipeUpdateFailure, Long> {
}

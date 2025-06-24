package com.haekitchenapp.recipeapp.repository;

import com.haekitchenapp.recipeapp.entity.RecipeStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RecipeStageRepository extends JpaRepository<RecipeStage, Long> {

    @Query("SELECT r FROM RecipeStage r JOIN FETCH r.ingredients WHERE r.id = :id")
    Optional<RecipeStage> findByIdWithIngredients(@Param("id") Long id);
}
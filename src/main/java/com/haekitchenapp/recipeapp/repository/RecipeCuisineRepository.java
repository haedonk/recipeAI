package com.haekitchenapp.recipeapp.repository;

import com.haekitchenapp.recipeapp.entity.composite.RecipeCuisine;
import com.haekitchenapp.recipeapp.entity.composite.RecipeCuisineId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RecipeCuisineRepository extends JpaRepository<RecipeCuisine, RecipeCuisineId> {

    List<RecipeCuisine> findByRecipeId(Long recipeId);

    List<RecipeCuisine> findByCuisineId(Integer cuisineId);

    @Transactional
    void deleteByRecipeId(Long recipeId);

    @Transactional
    void deleteByRecipeIdAndCuisineId(Long recipeId, Integer cuisineId);

    @Query("SELECT COUNT(rc) > 0 FROM RecipeCuisine rc WHERE rc.recipe.id = :recipeId AND rc.cuisine.id = :cuisineId")
    boolean existsByRecipeIdAndCuisineId(@Param("recipeId") Long recipeId, @Param("cuisineId") Integer cuisineId);
}

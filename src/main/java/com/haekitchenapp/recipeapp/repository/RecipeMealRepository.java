package com.haekitchenapp.recipeapp.repository;

import com.haekitchenapp.recipeapp.entity.RecipeMeal;
import com.haekitchenapp.recipeapp.entity.composite.RecipeMealId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipeMealRepository extends JpaRepository<RecipeMeal, RecipeMealId> {

    /**
     * Find all RecipeMeal entries for a specific recipe
     * @param recipeId The ID of the recipe
     * @return List of RecipeMeal objects associated with the recipe
     */
    List<RecipeMeal> findByRecipeId(Long recipeId);

    /**
     * Find all RecipeMeal entries for a specific meal type
     * @param mealTypeId The ID of the meal type
     * @return List of RecipeMeal objects associated with the meal type
     */
    List<RecipeMeal> findByMealTypeId(Short mealTypeId);

    /**
     * Delete all meal type associations for a specific recipe
     * @param recipeId The ID of the recipe
     */
    void deleteByRecipeId(Long recipeId);

    /**
     * Check if a recipe is associated with a specific meal type
     * @param recipeId The ID of the recipe
     * @param mealTypeId The ID of the meal type
     * @return true if the association exists, false otherwise
     */
    boolean existsByRecipeIdAndMealTypeId(Long recipeId, Short mealTypeId);

    /**
     * Count the number of recipes associated with a specific meal type
     * @param mealTypeId The ID of the meal type
     * @return The count of recipes
     */
    @Query("SELECT COUNT(rm) FROM RecipeMeal rm WHERE rm.mealType.id = :mealTypeId")
    long countRecipesByMealTypeId(@Param("mealTypeId") Short mealTypeId);
}

package com.haekitchenapp.recipeapp.repository;

import com.haekitchenapp.recipeapp.entity.RecipePlan;
import com.haekitchenapp.recipeapp.model.response.RecipePlanSimple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecipePlanRepository extends JpaRepository<RecipePlan, Long> {

    /**
     * Find all recipe plans for a specific user
     * @param userId The user ID
     * @return List of recipe plans
     */
    List<RecipePlan> findByUserId(Long userId);

    /**
     * Find all recipe plans for a specific user on a specific date
     * @param userId The user ID
     * @param planDate The plan date
     * @return List of recipe plans
     */
    List<RecipePlan> findByUserIdAndPlanDate(Long userId, LocalDate planDate);

    /**
     * Find all recipe plans for a specific user within a date range
     * @param userId The user ID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of recipe plans
     */
    @Query("SELECT new com.haekitchenapp.recipeapp.model.response.RecipePlanSimple(" +
           "rp.id, rp.user.id, rp.planDate, " +
           "rp.mealType.id," +
           "rp.recipe.id," +
           "rp.customTitle, rp.notes, rp.saved) " +
           "FROM RecipePlan rp WHERE rp.user.id = :userId AND rp.planDate BETWEEN :startDate AND :endDate " +
           "ORDER BY rp.planDate, rp.mealType.id")
    List<RecipePlanSimple> findByUserIdAndPlanDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find all recipe plans for a specific recipe
     * @param recipeId The recipe ID
     * @return List of recipe plans
     */
    List<RecipePlan> findByRecipeId(Long recipeId);

    /**
     * Find all recipe plans for a specific meal type
     * @param mealTypeId The meal type ID
     * @return List of recipe plans
     */
    List<RecipePlan> findByMealTypeId(Short mealTypeId);

    /**
     * Count the number of recipe plans for a specific recipe
     * @param recipeId The recipe ID
     * @return The count
     */
    long countByRecipeId(Long recipeId);

    /**
     * Delete all recipe plans for a specific user on a specific date
     * @param userId The user ID
     * @param planDate The plan date
     */
    void deleteByUserIdAndPlanDate(Long userId, LocalDate planDate);


    /**
     * Delete recipe plans by IDs and user ID
     * @param ids List of recipe plan IDs
     * @param userId The user ID
     */
    void deleteByIdInAndUserId(List<Long> ids, Long userId);

    /**
     * Find recipe plans by IDs and user ID
     * @param ids List of recipe plan IDs
     * @param userId The user ID
     * @return List of recipe plans
     */
    List<RecipePlan> findAllByIdInAndUserId(List<Long> ids, Long userId);

    /**
     * Update saved status for all recipe plans between two dates for a user
     *
     * @param userId      The user ID
     * @param startDate   Start date (inclusive)
     * @param endDate     End date (inclusive)
     * @param savedStatus The saved status to set
     */
    @Modifying
    @Query("UPDATE RecipePlan rp SET rp.saved = :savedStatus WHERE rp.user.id = :userId AND rp.planDate BETWEEN :startDate AND :endDate")
    void updateSavedStatusBetweenDates(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("savedStatus") Boolean savedStatus);
}

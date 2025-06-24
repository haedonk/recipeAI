package com.haekitchenapp.recipeapp.repository;

import com.haekitchenapp.recipeapp.entity.Recipe;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeDuplicatesByTitleDto;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeSimilarityDto;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    @Query("SELECT new com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto(r.id,r.title,r.instructions) FROM Recipe r WHERE r.title = :title")
    List<RecipeTitleDto> findIdsByTitle(String title);

    @Query("SELECT new com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto(r.id,r.title,r.instructions) FROM Recipe r WHERE r.id = :id")
    Optional<RecipeTitleDto> findRecipeTitleDtoById(Long id);

    @Query("SELECT r FROM Recipe r LEFT JOIN FETCH r.ingredients ORDER BY r.id")
    Page<Recipe> findAllWithIngredients(Pageable pageable);

    @Query("SELECT new com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto(r.id, r.title) FROM Recipe r WHERE LOWER(r.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<RecipeTitleDto> findTitlesByTitleContainingIgnoreCase(@Param("title") String title, Pageable pageable);

    @Query("SELECT r FROM Recipe r JOIN FETCH r.ingredients WHERE r.id = :id")
    Optional<Recipe> findByIdWithIngredients(@Param("id") Long id);

    @Query("SELECT new com.haekitchenapp.recipeapp.model.response.recipe.RecipeDuplicatesByTitleDto(r.title, COUNT(r.title)) " +
            "FROM Recipe r " +
            "GROUP BY r.title " +
            "HAVING COUNT(r.title) > 1 ORDER BY COUNT(r.title) DESC")
    Page<RecipeDuplicatesByTitleDto> findDuplicateTitles(Pageable pageable);

    @Query("SELECT new com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto(r.id,r.title,r.instructions) FROM Recipe r WHERE r.createdBy = :userId")
    List<RecipeTitleDto> findTitlesByCreatedBy(Long userId);

    @Query(value = "SELECT COUNT(*) FROM recipes WHERE embedding IS NOT NULL", nativeQuery = true)
    Long countByEmbeddingIsNotNull();

    @Query(value = """
    SELECT id, title, summary, (embedding <#> cast(:queryVector as vector)) AS similarity
    FROM recipes
    ORDER BY similarity ASC
    LIMIT :limit
    """, nativeQuery = true)
    List<RecipeSimilarityDto> findTopByEmbeddingSimilarity(@Param("queryVector") String queryVector, @Param("limit") int limit);

    @Modifying
    @Query(value = "UPDATE recipes SET embedding = cast(:vector AS vector) WHERE id = :id", nativeQuery = true)
    void updateEmbedding(@Param("id") Long id, @Param("vector") String vector);
}
package com.haekitchenapp.recipeapp.repository;

import com.haekitchenapp.recipeapp.entity.Recipe;
import com.haekitchenapp.recipeapp.model.response.recipe.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    @Query("SELECT new com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto(r.id,r.title,r.instructions) FROM Recipe r WHERE r.title = :title")
    List<RecipeTitleDto> findIdsByTitle(String title);

    @Query("SELECT new com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto(r.id,r.title,r.instructions) FROM Recipe r WHERE r.id = :id")
    Optional<RecipeTitleDto> findRecipeTitleDtoById(Long id);

    @Query("SELECT new com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleSummaryDto(r.id,r.title, r.summary) FROM Recipe r WHERE r.id = :id")
    Optional<RecipeTitleSummaryDto> findRecipeTitleSummaryDtoById(Long id);

    @Query("SELECT new com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto(r.id, r.title) FROM Recipe r WHERE LOWER(r.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<RecipeTitleDto> findTitlesByTitleContainingIgnoreCase(@Param("title") String title, Pageable pageable);

    @Query("SELECT r FROM Recipe r JOIN FETCH r.ingredients WHERE r.id = :id")
    Optional<Recipe> findByIdWithIngredients(@Param("id") Long id);

    @Query("SELECT r FROM Recipe r WHERE r.id = :id")
    Optional<RecipeSummaryProjection> findByIdWithSimple(@Param("id") Long id);

    @Query("SELECT new com.haekitchenapp.recipeapp.model.response.recipe.RecipeDuplicatesByTitleDto(r.title, COUNT(r.title)) " +
            "FROM Recipe r " +
            "GROUP BY r.title " +
            "HAVING COUNT(r.title) > 1 ORDER BY COUNT(r.title) DESC")
    Page<RecipeDuplicatesByTitleDto> findDuplicateTitles(Pageable pageable);

    @Query("SELECT new com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleSummaryDto(r.id,r.title,r.summary) FROM Recipe r WHERE r.createdBy = :userId")
    List<RecipeTitleSummaryDto> findTitlesByCreatedBy(Long userId);

    @Query(value = "SELECT COUNT(*) FROM recipes WHERE embedding IS NOT NULL", nativeQuery = true)
    Long countByEmbeddingIsNotNull();

    @Query(value = """
            WITH _probes AS (
              SELECT set_config('ivfflat.probes','20', true)
            )
            SELECT r.id,
                   r.title,
                   r.summary,
                   (r.embedding <=> CAST(:queryVector AS vector))       AS cosine_distance,
                   1 - (r.embedding <=> CAST(:queryVector AS vector))   AS similarity
            FROM public.recipes r, _probes
            ORDER BY r.embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<RecipeSimilarityView> findTopByCosine(
            @Param("queryVector") String queryVector,
            @Param("limit") int limit
    );

    @Query(value = """
            WITH _probes AS (
              SELECT set_config('ivfflat.probes','20', true)
            ),
            cand AS (
              SELECT r.id, r.title, r.summary,
                     (r.embedding <=> CAST(:queryVector AS vector))       AS cosine_distance,
                     1 - (r.embedding <=> CAST(:queryVector AS vector))   AS similarity
              FROM public.recipes r, _probes
              ORDER BY r.embedding <=> CAST(:queryVector AS vector)
              LIMIT 200
            )
            SELECT id, title, summary, cosine_distance, similarity
            FROM cand
            ORDER BY CASE WHEN lower(title) LIKE lower(:titlePattern) THEN 0 ELSE 1 END,
                     cosine_distance
            LIMIT :limit
            """, nativeQuery = true)
    List<RecipeSimilarityView> findTopByCosineWithTitle(
            @Param("queryVector") String queryVector,
            @Param("limit") int limit,
            @Param("titlePattern") String titlePattern
    );

    @Modifying
    @Query(value = "UPDATE recipes SET embedding = cast(:vector AS vector) WHERE id = :id", nativeQuery = true)
    void updateEmbedding(@Param("id") Long id, @Param("vector") String vector);
}
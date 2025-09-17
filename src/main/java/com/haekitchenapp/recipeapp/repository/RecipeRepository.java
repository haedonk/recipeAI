package com.haekitchenapp.recipeapp.repository;

import com.haekitchenapp.recipeapp.entity.Recipe;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeDuplicatesByTitleDto;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeSimilarityDto;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeSummaryProjection;
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

    @Query("SELECT new com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto(r.id,r.title,r.instructions) FROM Recipe r WHERE r.createdBy = :userId")
    List<RecipeTitleDto> findTitlesByCreatedBy(Long userId);

    @Query(value = "SELECT COUNT(*) FROM recipes WHERE embedding IS NOT NULL", nativeQuery = true)
    Long countByEmbeddingIsNotNull();

    @Query(value = """
    WITH _probes AS (
      SELECT set_config('ivfflat.probes','10', true)  -- true = SET LOCAL (transaction-scoped)
    ),
    cand AS (
      SELECT r.id, r.title, r.summary,
             (r.embedding <#> CAST(:queryVector AS vector)) AS dist
      FROM public.recipes r, _probes
      ORDER BY r.embedding <#> CAST(:queryVector AS vector)
      LIMIT 200
    )
    SELECT id, title, summary, dist
    FROM cand
    ORDER BY
      CASE WHEN lower(title) LIKE lower(:titlePattern) THEN 0 ELSE 1 END,
      dist
    LIMIT :limit
    """, nativeQuery = true)
    List<RecipeSimilarityDto> findTopByEmbeddingSimilarityAndTitle(@Param("queryVector") String queryVector, @Param("limit") int limit, @Param("titlePattern") String titlePattern);

    @Query(value = """
    WITH _probes AS (
      SELECT set_config('ivfflat.probes','10', true)  -- true = SET LOCAL (transaction-scoped)
    ),
    cand AS (
      SELECT r.id, r.title, r.summary,
             (r.embedding <#> CAST(:queryVector AS vector)) AS dist
      FROM public.recipes r, _probes
      ORDER BY r.embedding <#> CAST(:queryVector AS vector)
      LIMIT 200
    )
    SELECT id, title, summary, dist
    FROM cand
    ORDER BY dist
    LIMIT :limit
    """, nativeQuery = true)
    List<RecipeSimilarityDto> findTopByEmbeddingSimilarity(
            @Param("queryVector") String queryVector,
            @Param("limit") int limit
    );

    @Modifying
    @Query(value = "UPDATE recipes SET embedding = cast(:vector AS vector) WHERE id = :id", nativeQuery = true)
    void updateEmbedding(@Param("id") Long id, @Param("vector") String vector);
}
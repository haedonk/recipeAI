package com.haekitchenapp.recipeapp.repository;

import com.haekitchenapp.recipeapp.entity.Recipe;
import com.haekitchenapp.recipeapp.model.response.RecipeDetailsDto;
import com.haekitchenapp.recipeapp.model.response.RecipeTitleDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByTitleContainingIgnoreCase(String title, PageRequest pageable);


    @Query("SELECT r FROM Recipe r LEFT JOIN FETCH r.ingredients ORDER BY r.id")
    Page<Recipe> findAllWithIngredients(Pageable pageable);

    @Query("SELECT new com.haekitchenapp.recipeapp.model.response.RecipeTitleDto(r.id, r.title) FROM Recipe r WHERE LOWER(r.title) LIKE LOWER(:title || '%')")
    List<RecipeTitleDto> findTitlesByTitleContainingIgnoreCase(@Param("title") String title, Pageable pageable);

    @Query("SELECT r FROM Recipe r JOIN FETCH r.ingredients WHERE r.id = :id")
    Optional<Recipe> findByIdWithIngredients(@Param("id") Long id);


}
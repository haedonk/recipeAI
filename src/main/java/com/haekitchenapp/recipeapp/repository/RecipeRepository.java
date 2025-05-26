package com.haekitchenapp.recipeapp.repository;

import com.haekitchenapp.recipeapp.entity.Recipe;
import com.haekitchenapp.recipeapp.model.response.RecipeTitleDto;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByTitleContainingIgnoreCase(String title, PageRequest pageable);


    @Query("SELECT new com.haekitchenapp.recipeapp.model.response.RecipeTitleDto(r.id, r.title) FROM Recipe r WHERE LOWER(r.title) LIKE LOWER(:title || '%')")
    List<RecipeTitleDto> findTitlesByTitleContainingIgnoreCase(@Param("title") String title, Pageable pageable);
}
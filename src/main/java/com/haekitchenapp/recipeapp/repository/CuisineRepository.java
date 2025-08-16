package com.haekitchenapp.recipeapp.repository;

import com.haekitchenapp.recipeapp.entity.Cuisine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CuisineRepository extends JpaRepository<Cuisine, Integer> {

    Optional<Cuisine> findByName(String name);

    List<Cuisine> findByNameContainingIgnoreCase(String name);

    @Query("SELECT c FROM Cuisine c JOIN c.recipes rc WHERE rc.recipe.id = :recipeId")
    List<Cuisine> findByRecipeId(@Param("recipeId") Long recipeId);

    boolean existsByName(String name);
}

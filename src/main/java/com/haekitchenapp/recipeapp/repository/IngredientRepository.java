package com.haekitchenapp.recipeapp.repository;

import com.haekitchenapp.recipeapp.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    Optional<Ingredient> findByNameIgnoreCase(String name);

    @Query("SELECT i FROM Ingredient i WHERE i.id > :id")
    List<Ingredient> findAllGreaterThanId(@Param("id") Long id);
}

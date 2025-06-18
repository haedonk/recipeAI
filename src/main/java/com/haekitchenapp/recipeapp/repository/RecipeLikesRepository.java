package com.haekitchenapp.recipeapp.repository;

import com.haekitchenapp.recipeapp.entity.RecipeLikes;
import com.haekitchenapp.recipeapp.entity.composite.RecipeLikesId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface RecipeLikesRepository extends JpaRepository<RecipeLikes, RecipeLikesId> {
    List<RecipeLikes> findByIdUserId(Long userId);
    List<RecipeLikes> findByIdRecipeId(Long recipeId);
}

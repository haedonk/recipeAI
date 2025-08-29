package com.haekitchenapp.recipeapp.service.impl;

import com.haekitchenapp.recipeapp.entity.Cuisine;
import com.haekitchenapp.recipeapp.entity.Recipe;
import com.haekitchenapp.recipeapp.entity.composite.RecipeCuisine;
import com.haekitchenapp.recipeapp.entity.composite.RecipeCuisineId;
import com.haekitchenapp.recipeapp.exception.CuisineNotFoundException;
import com.haekitchenapp.recipeapp.repository.CuisineRepository;
import com.haekitchenapp.recipeapp.repository.RecipeCuisineRepository;
import com.haekitchenapp.recipeapp.repository.RecipeRepository;
import com.haekitchenapp.recipeapp.service.RecipeCuisineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecipeCuisineServiceImpl implements RecipeCuisineService {

    private final RecipeCuisineRepository recipeCuisineRepository;
    private final RecipeRepository recipeRepository;
    private final CuisineRepository cuisineRepository;

    @Override
    @Transactional
    public RecipeCuisine associateRecipeWithCuisine(Long recipeId, Integer cuisineId) {
        log.info("Associating recipe ID: {} with cuisine ID: {}", recipeId, cuisineId);

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new CuisineNotFoundException("Recipe not found with id: " + recipeId));

        Cuisine cuisine = cuisineRepository.findById(cuisineId)
                .orElseThrow(() -> new CuisineNotFoundException("Cuisine not found with id: " + cuisineId));

        // Check if association already exists
        if (recipeCuisineRepository.existsByRecipeIdAndCuisineId(recipeId, cuisineId)) {
            log.info("Association already exists for recipe ID: {} and cuisine ID: {}", recipeId, cuisineId);
            RecipeCuisineId id = new RecipeCuisineId(recipeId, cuisineId);
            return recipeCuisineRepository.findById(id)
                    .orElseThrow(() -> new IllegalStateException("Association should exist but not found"));
        }

        RecipeCuisine recipeCuisine = new RecipeCuisine();
        recipeCuisine.setId(new RecipeCuisineId(recipeId, cuisineId));
        recipeCuisine.setRecipe(recipe);
        recipeCuisine.setCuisine(cuisine);

        return recipeCuisineRepository.save(recipeCuisine);
    }

    @Override
    @Transactional
    public void associateRecipeWithCuisines(Long recipeId, Set<Integer> cuisineIds) {
        log.info("Associating recipe ID: {} with multiple cuisines: {}", recipeId, cuisineIds);

        if (!recipeRepository.existsById(recipeId)) {
            throw new CuisineNotFoundException("Recipe not found with id: " + recipeId);
        }

        for (Integer cuisineId : cuisineIds) {
            if (!recipeCuisineRepository.existsByRecipeIdAndCuisineId(recipeId, cuisineId)) {
                Cuisine cuisine = cuisineRepository.findById(cuisineId)
                        .orElseThrow(() -> new CuisineNotFoundException("Cuisine not found with id: " + cuisineId));

                RecipeCuisine recipeCuisine = new RecipeCuisine();
                recipeCuisine.setId(new RecipeCuisineId(recipeId, cuisineId));

                // We'll need to fetch the recipe for the relationship
                Recipe recipe = recipeRepository.getReferenceById(recipeId);
                recipeCuisine.setRecipe(recipe);
                recipeCuisine.setCuisine(cuisine);

                recipeCuisineRepository.save(recipeCuisine);
                log.info("Successfully associated recipe ID: {} with cuisine ID: {}", recipeId, cuisineId);
            } else {
                log.info("Association already exists for recipe ID: {} and cuisine ID: {}", recipeId, cuisineId);
            }
        }
    }

    @Override
    public List<RecipeCuisine> getRecipeCuisines(Long recipeId) {
        log.info("Getting cuisine associations for recipe ID: {}", recipeId);

        if (!recipeRepository.existsById(recipeId)) {
            throw new CuisineNotFoundException("Recipe not found with id: " + recipeId);
        }

        return recipeCuisineRepository.findByRecipeId(recipeId);
    }

    @Override
    public List<RecipeCuisine> getCuisineRecipes(Integer cuisineId) {
        log.info("Getting recipe associations for cuisine ID: {}", cuisineId);

        if (!cuisineRepository.existsById(cuisineId)) {
            throw new CuisineNotFoundException("Cuisine not found with id: " + cuisineId);
        }

        return recipeCuisineRepository.findByCuisineId(cuisineId);
    }

    @Override
    @Transactional
    public void removeRecipeCuisine(Long recipeId, Integer cuisineId) {
        log.info("Removing association for recipe ID: {} and cuisine ID: {}", recipeId, cuisineId);

        if (!recipeCuisineRepository.existsByRecipeIdAndCuisineId(recipeId, cuisineId)) {
            log.warn("Association doesn't exist for recipe ID: {} and cuisine ID: {}", recipeId, cuisineId);
            return;
        }

        recipeCuisineRepository.deleteByRecipeIdAndCuisineId(recipeId, cuisineId);
    }

    @Override
    @Transactional
    public void removeAllRecipeCuisines(Long recipeId) {
        log.info("Removing all cuisine associations for recipe ID: {}", recipeId);

        if (!recipeRepository.existsById(recipeId)) {
            throw new CuisineNotFoundException("Recipe not found with id: " + recipeId);
        }

        recipeCuisineRepository.deleteByRecipeId(recipeId);
    }

    @Override
    public boolean isRecipeAssociatedWithCuisine(Long recipeId, Integer cuisineId) {
        return recipeCuisineRepository.existsByRecipeIdAndCuisineId(recipeId, cuisineId);
    }

    @Override
    public List<Cuisine> getRecipeCuisineList(Long recipeId) {
        log.info("Getting cuisines for recipe ID: {}", recipeId);

        if (!recipeRepository.existsById(recipeId)) {
            throw new CuisineNotFoundException("Recipe not found with id: " + recipeId);
        }

        return cuisineRepository.findByRecipeId(recipeId);
    }

    @Override
    @Transactional
    public void updateRecipeCuisines(Long recipeId, Set<Integer> cuisineIds) {
        log.info("Updating cuisines for recipe ID: {} with cuisines: {}", recipeId, cuisineIds);

        if (!recipeRepository.existsById(recipeId)) {
            throw new CuisineNotFoundException("Recipe not found with id: " + recipeId);
        }

        // Remove existing associations
        recipeCuisineRepository.deleteByRecipeId(recipeId);

        // Create new associations
        associateRecipeWithCuisines(recipeId, cuisineIds);
    }
}

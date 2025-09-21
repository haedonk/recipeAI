package com.haekitchenapp.recipeapp.service;

import com.haekitchenapp.recipeapp.entity.MealType;
import com.haekitchenapp.recipeapp.entity.Recipe;
import com.haekitchenapp.recipeapp.entity.RecipeMeal;
import com.haekitchenapp.recipeapp.entity.composite.RecipeMealId;
import com.haekitchenapp.recipeapp.repository.MealTypeRepository;
import com.haekitchenapp.recipeapp.repository.RecipeMealRepository;
import com.haekitchenapp.recipeapp.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeMealService {

    private final RecipeMealRepository recipeMealRepository;
    private final RecipeRepository recipeRepository;
    private final MealTypeRepository mealTypeRepository;

    /**
     * Insert a new association between a recipe and a meal type
     *
     * @param recipe the recipe entity
     * @param mealType the meal type entity
     * @return the saved RecipeMeal entity
     */
    @Transactional
    public RecipeMeal insert(Recipe recipe, MealType mealType) {
        // Check if the association already exists to avoid duplicates
        if (recipeMealRepository.existsById(new RecipeMealId(recipe.getId(), mealType.getId()))) {
            return null; // or throw an exception if you prefer
        }

        RecipeMeal recipeMeal = new RecipeMeal();

        // Set both the ID and the entity references
        recipeMeal.setId(new RecipeMealId(recipe.getId(), mealType.getId()));
        recipeMeal.setRecipe(recipe);
        recipeMeal.setMealType(mealType);

        return recipeMealRepository.save(recipeMeal);
    }

    @Transactional
    public void associateRecipeWithmealTypes(Long recipe_id, Set<Short> meal_ids) {

        boolean recipeExists = recipeRepository.existsById(recipe_id);
        boolean mealTypeExists = meal_ids.stream().allMatch(mealTypeRepository::existsById);

        if (!recipeExists || !mealTypeExists) {
            throw new IllegalArgumentException("Recipe or MealType does not exist");
        } else {
            log.info("Associating recipe ID: {} with meal types: {}", recipe_id, meal_ids);
        }

        // Get the recipe entity once outside the loop
        Recipe recipe = recipeRepository.getReferenceById(recipe_id);

        for (Short mealId : meal_ids) {
            if (!recipeMealRepository.existsById(new RecipeMealId(recipe_id, mealId))) {
                // Get the meal type entity
                MealType mealType = mealTypeRepository.getReferenceById(mealId);

                RecipeMeal recipeMeal = new RecipeMeal();

                // Set both the ID and the entity references
                recipeMeal.setId(new RecipeMealId(recipe_id, mealId));
                recipeMeal.setRecipe(recipe);
                recipeMeal.setMealType(mealType);

                recipeMealRepository.save(recipeMeal);
            }
        }
    }
}

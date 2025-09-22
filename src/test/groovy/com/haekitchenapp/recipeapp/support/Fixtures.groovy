package com.haekitchenapp.recipeapp.support

import com.haekitchenapp.recipeapp.entity.Ingredient
import com.haekitchenapp.recipeapp.entity.Recipe
import com.haekitchenapp.recipeapp.entity.RecipeIngredient
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeIngredientRequest
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeRequest
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeDetailsDto
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeDuplicatesByTitleDto
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeSummaryProjection
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto

class Fixtures {

    static RecipeRequest recipeRequest(Map overrides = [:]) {
        new RecipeRequest(
                overrides.get('id') as Long,
                overrides.getOrDefault('createdBy', 10L) as Long,
                overrides.getOrDefault('title', 'Test Recipe') as String,
                overrides.getOrDefault('instructions', 'Mix ingredients and cook') as String,
                overrides.get('embedding') as Double[],
                overrides.getOrDefault('summary', 'Tasty meal') as String,
                (overrides.get('ingredients') ?: [recipeIngredientRequest()].toSet()) as Set<RecipeIngredientRequest>,
                overrides.getOrDefault('prepTime', 15) as Integer,
                overrides.getOrDefault('cookTime', 20) as Integer,
                overrides.getOrDefault('servings', 4) as Integer,
                overrides.getOrDefault('aiGenerated', false) as Boolean,
                overrides.get('cleanedFrom') as Long
        )
    }

    static RecipeIngredientRequest recipeIngredientRequest(Map overrides = [:]) {
        new RecipeIngredientRequest(
                overrides.get('id') as Long,
                overrides.getOrDefault('name', 'Salt') as String,
                overrides.getOrDefault('quantity', '1') as String,
                overrides.getOrDefault('unitId', 1L) as Long
        ).tap {
            unitName = overrides.get('unitName') as String
        }
    }

    static Recipe recipe(Map overrides = [:]) {
        Recipe recipe = new Recipe()
        recipe.id = overrides.get('id') as Long
        recipe.title = overrides.getOrDefault('title', 'Test Recipe') as String
        recipe.instructions = overrides.getOrDefault('instructions', 'Mix ingredients and cook') as String
        recipe.summary = overrides.getOrDefault('summary', 'Tasty meal') as String
        recipe.prepTime = overrides.getOrDefault('prepTime', 15) as Integer
        recipe.cookTime = overrides.getOrDefault('cookTime', 20) as Integer
        recipe.servings = overrides.getOrDefault('servings', 4) as Integer
        recipe.createdBy = overrides.getOrDefault('createdBy', 10L) as Long
        recipe.aiGenerated = overrides.getOrDefault('aiGenerated', false) as Boolean
        recipe.cleanedFrom = overrides.get('cleanedFrom') as Long

        if (overrides.containsKey('ingredients')) {
            recipe.ingredients = overrides.get('ingredients') as Set<RecipeIngredient>
        }

        recipe
    }

    static RecipeIngredient recipeIngredient(Recipe recipe, Ingredient ingredient, Map overrides = [:]) {
        RecipeIngredient recipeIngredient = new RecipeIngredient()
        recipeIngredient.recipe = recipe
        recipeIngredient.ingredient = ingredient
        recipeIngredient.quantity = overrides.getOrDefault('quantity', 1F) as Float
        recipeIngredient.unitId = overrides.getOrDefault('unitId', 1L) as Long
        recipeIngredient
    }

    static Ingredient ingredient(String name = 'Salt') {
        Ingredient ingredient = new Ingredient()
        ingredient.name = name
        ingredient
    }

    static RecipeTitleDto recipeTitleDto(Long id = 1L, String title = 'Test Recipe', String instructions = 'Cook well') {
        new RecipeTitleDto(id, title, instructions)
    }

    static RecipeDuplicatesByTitleDto duplicatesDto(String title = 'Test Recipe', Long count = 2L) {
        new RecipeDuplicatesByTitleDto(title, count)
    }

    static RecipeSummaryProjection recipeSummaryProjection(String title = 'Test Recipe', String instructions = 'Cook well') {
        [
                getTitle      : { title },
                getInstructions: { instructions }
        ] as RecipeSummaryProjection
    }

    static RecipeDetailsDto recipeDetailsDto(Map overrides = [:]) {
        new RecipeDetailsDto(
                overrides.getOrDefault('title', 'Test Recipe') as String,
                overrides.getOrDefault('ingredients', ['Salt', 'Pepper']) as List<String>,
                overrides.getOrDefault("cuisines", ['Italian']) as List<String>,
                overrides.getOrDefault('instructions', 'Cook well') as String,
                overrides.getOrDefault('id', 1L) as Long
        )
    }
}

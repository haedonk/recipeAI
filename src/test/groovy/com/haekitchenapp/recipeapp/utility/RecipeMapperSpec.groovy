package com.haekitchenapp.recipeapp.utility

import com.haekitchenapp.recipeapp.entity.Ingredient
import com.haekitchenapp.recipeapp.entity.Recipe
import com.haekitchenapp.recipeapp.entity.RecipeIngredient
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeIngredientRequest
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeRequest
import com.haekitchenapp.recipeapp.repository.IngredientRepository
import spock.lang.Specification

class RecipeMapperSpec extends Specification {
    def ingredientRepo = Mock(IngredientRepository)
    def mapper = new RecipeMapper(ingredientRepo)

    def "toEntity maps request to entity"() {
        given:
        def req = new RecipeRequest(null, "T", "I", null,
                [new RecipeIngredientRequest(null, "Salt", "1", "tsp" )] as Set,
                5, 10, 2)
        ingredientRepo.findByNameIgnoreCase("Salt") >> Optional.of(new Ingredient(id:1,name:"Salt"))

        when:
        def recipe = mapper.toEntity(req)

        then:
        recipe.title == "T"
        recipe.ingredients.size() == 1
    }

    def "toLlmDetailsDto builds dto"() {
        given:
        def ing = new Ingredient(id:1,name:"Salt")
        def ri = new RecipeIngredient(ingredient:ing)
        def rec = new Recipe(title:"Soup", instructions:"Mix", ingredients:[ri] as Set)

        when:
        def dto = mapper.toLlmDetailsDto(rec)

        then:
        dto.title == "Soup"
        dto.ingredients == ["Salt"]
        dto.instructions == "Mix"
    }
}

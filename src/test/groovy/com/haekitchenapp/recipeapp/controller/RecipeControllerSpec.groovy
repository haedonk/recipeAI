package com.haekitchenapp.recipeapp.controller

import com.haekitchenapp.recipeapp.exception.RecipeNotFoundException
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeRequest
import com.haekitchenapp.recipeapp.model.response.ApiResponse
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeResponse
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto
import com.haekitchenapp.recipeapp.service.RecipeService
import com.haekitchenapp.recipeapp.service.UnitService
import com.haekitchenapp.recipeapp.support.Fixtures
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class RecipeControllerSpec extends Specification {

    RecipeController recipeController
    RecipeService recipeService
    UnitService unitService

    def setup() {
        recipeService = Mock(RecipeService)
        unitService = Mock(UnitService)
        recipeController = new RecipeController(recipeService, unitService)
    }

    def "creates recipe when payload is valid"() {
        given:
        RecipeRequest request = Fixtures.recipeRequest()
        def recipe = Fixtures.recipe(id: 55L)
        recipeService.create(_ as RecipeRequest) >> ResponseEntity.ok(ApiResponse.success('Recipe created successfully', recipe))

        when:
        def response = recipeController.createRecipe(request)

        then:
        response.statusCode.value() == 200
        response.body.success
        response.body.data.id == 55L
    }

    def "gets recipe by id when it exists"() {
        given:
        def responseBody = new RecipeResponse(10L, 1L, 'Chili', 'Cook slowly', 'Spicy', null, [] as Set, 5, 30, 4)
        recipeService.findById(10L) >> ResponseEntity.ok(ApiResponse.success('Recipe retrieved successfully', responseBody))

        when:
        def response = recipeController.getRecipeById(10L)

        then:
        response.statusCode.value() == 200
        response.body.data.title == 'Chili'
    }

    def "returns 404 when recipe is not found"() {
        given:
        recipeService.findById(100L) >> { throw new RecipeNotFoundException('Recipe not found with ID: 100') }

        when:
        recipeController.getRecipeById(100L)

        then:
        thrown(RecipeNotFoundException)
    }

    def "searches recipes by title"() {
        given:
        def dtos = [new RecipeTitleDto(1L, 'Toast', 'Instructions')]
        recipeService.searchByTitle('Toast') >> ResponseEntity.ok(ApiResponse.success('Recipes retrieved successfully', dtos))

        when:
        def response = recipeController.searchRecipesByTitle('Toast')

        then:
        response.statusCode.value() == 200
        response.body.data[0].title == 'Toast'
    }

    def "returns error when search title is blank"() {
        given:
        recipeService.searchByTitle('') >> { throw new IllegalArgumentException('Title must not be null or empty') }

        when:
        recipeController.searchRecipesByTitle('')

        then:
        thrown(IllegalArgumentException)
    }

    def "deletes recipe by id"() {
        given:
        recipeService.deleteById(5L) >> ResponseEntity.ok(ApiResponse.success('Recipe deleted successfully'))

        when:
        def response = recipeController.deleteRecipe(5L)

        then:
        response.statusCode.value() == 200
        response.body.message == 'Recipe deleted successfully'
    }

}

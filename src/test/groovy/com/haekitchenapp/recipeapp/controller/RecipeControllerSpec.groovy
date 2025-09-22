package com.haekitchenapp.recipeapp.controller

import com.haekitchenapp.recipeapp.entity.Recipe
import com.haekitchenapp.recipeapp.entity.Unit
import com.haekitchenapp.recipeapp.exception.RecipeNotFoundException
import com.haekitchenapp.recipeapp.exception.RecipeSearchFoundNoneException
import com.haekitchenapp.recipeapp.model.request.recipe.EmbedUpdateRequest
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeRequest
import com.haekitchenapp.recipeapp.model.response.ApiResponse
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeDetailsDto
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeDuplicatesByTitleDto
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeDuplicatesByTitleResponse
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeResponse
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto
import com.haekitchenapp.recipeapp.service.JwtTokenService
import com.haekitchenapp.recipeapp.service.RecipeService
import com.haekitchenapp.recipeapp.service.UnitService
import com.haekitchenapp.recipeapp.support.Fixtures
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class RecipeControllerSpec extends Specification {

    RecipeController recipeController
    RecipeService recipeService
    UnitService unitService
    JwtTokenService jwtTokenService

    def setup() {
        recipeService = Mock(RecipeService)
        unitService = Mock(UnitService)
        jwtTokenService = Mock(JwtTokenService)
        recipeController = new RecipeController(recipeService, unitService, jwtTokenService)
    }

    def "creates recipe when payload is valid"() {
        given:
        RecipeRequest request = Fixtures.recipeRequest()
        def created = Fixtures.recipe(id: 55L)

        when:
        def response = recipeController.createRecipe(request)

        then:
        1 * recipeService.create({ RecipeRequest arg ->
            assert arg.is(request)
            true
        }) >> ResponseEntity.ok(ApiResponse.success("Recipe created successfully", created))
        0 * _
        response.statusCode == HttpStatus.OK
        response.body.success
        response.body.message == "Recipe created successfully"
        response.body.data.id == 55L
    }

    def "bulk creates recipes when payload is valid"() {
        given:
        List<RecipeRequest> requests = [
                Fixtures.recipeRequest([title: 'First']),
                Fixtures.recipeRequest([title: 'Second'])
        ]
        List<Recipe> created = [
                Fixtures.recipe([id: 1L, title: 'First']),
                Fixtures.recipe([id: 2L, title: 'Second'])
        ]

        when:
        def response = recipeController.createRecipes(requests)

        then:
        1 * recipeService.createBulk({ List<RecipeRequest> args ->
            assert args*.title == ['First', 'Second']
            true
        }) >> ResponseEntity.ok(ApiResponse.success("Recipes created successfully", created))
        0 * _
        response.statusCode == HttpStatus.OK
        response.body.success
        response.body.data*.title == ['First', 'Second']
    }

    def "updates recipe injecting path id"() {
        given:
        RecipeRequest request = Fixtures.recipeRequest([id: null, title: 'Updated title'])

        when:
        def response = recipeController.updateRecipe(99L, request)

        then:
        1 * recipeService.update({ RecipeRequest arg ->
            assert arg.is(request)
            assert arg.id == 99L
            true
        }) >> ResponseEntity.ok(ApiResponse.success("Recipe updated successfully", Fixtures.recipe(id: 99L, title: 'Updated title')))
        0 * _
        response.statusCode == HttpStatus.OK
        response.body.success
        response.body.data.id == 99L
        request.id == 99L
    }

    def "updates recipe embedding when payload is valid"() {
        given:
        EmbedUpdateRequest request = new EmbedUpdateRequest(id: 22L, embedding: [0.1D, 0.2D] as Double[])

        when:
        def response = recipeController.updateRecipe(request)

        then:
        1 * recipeService.updateEmbeddingOnly({ EmbedUpdateRequest arg ->
            assert arg.is(request)
            true
        }) >> ResponseEntity.ok(ApiResponse.success("Recipe embedding updated successfully"))
        0 * _
        response.statusCode == HttpStatus.OK
        response.body.success
        response.body.message == "Recipe embedding updated successfully"
    }

    def "gets recipe by id when it exists"() {
        given:
        def recipeResponse = new RecipeResponse(10L, 1L, 'Chili', 'Cook slowly', 'Spicy', null, [] as Set, 5, 30, 4)

        when:
        def response = recipeController.getRecipeById(10L)

        then:
        1 * recipeService.findById(10L) >> ResponseEntity.ok(ApiResponse.success("Recipe retrieved successfully", recipeResponse))
        0 * _
        response.statusCode == HttpStatus.OK
        response.body.data.title == 'Chili'
    }

    def "throws RecipeNotFoundException when recipe does not exist"() {
        when:
        recipeController.getRecipeById(404L)

        then:
        1 * recipeService.findById(404L) >> { throw new RecipeNotFoundException('Recipe not found with ID: 404') }
        0 * _
        def ex = thrown(RecipeNotFoundException)
        ex.message == 'Recipe not found with ID: 404'
    }

    def "searches recipes by title"() {
        given:
        List<RecipeTitleDto> titles = [new RecipeTitleDto(7L, 'Toast', 'Brown it')]

        when:
        def response = recipeController.searchRecipesByTitle('Toast')

        then:
        1 * recipeService.searchByTitle('Toast') >> ResponseEntity.ok(ApiResponse.success("Recipes retrieved successfully", titles))
        0 * _
        response.statusCode == HttpStatus.OK
        response.body.data*.id == [7L]
        response.body.data*.instructions == ['Brown it']
    }

    def "throws RecipeSearchFoundNoneException when search finds nothing"() {
        when:
        recipeController.searchRecipesByTitle('Missing')

        then:
        1 * recipeService.searchByTitle('Missing') >> { throw new RecipeSearchFoundNoneException('No recipes found with title: Missing') }
        0 * _
        def ex = thrown(RecipeSearchFoundNoneException)
        ex.message == 'No recipes found with title: Missing'
    }

    def "lists duplicate recipes by page"() {
        given:
        def duplicates = new RecipeDuplicatesByTitleResponse([new RecipeDuplicatesByTitleDto('Toast', 3L)], true)

        when:
        def response = recipeController.getDuplicateRecipes(1)

        then:
        1 * recipeService.findDuplicateTitles(1) >> ResponseEntity.ok(ApiResponse.success("Duplicate titles retrieved successfully", duplicates))
        0 * _
        response.statusCode == HttpStatus.OK
        response.body.data.duplicates*.title == ['Toast']
        response.body.data.isLastPage
    }

    def "retrieves recipe details"() {
        given:
        def details = Fixtures.recipeDetailsDto([title: 'Toast'])

        when:
        def response = recipeController.getRecipeDetails(12L)

        then:
        1 * recipeService.getRecipeDetailsResponse(12L) >> ResponseEntity.ok(ApiResponse.success("Recipe retrieved successfully", details))
        0 * _
        response.statusCode == HttpStatus.OK
        response.body.data.title == 'Toast'
    }

    def "retrieves units"() {
        given:
        List<Unit> units = [new Unit(id: 1L, name: 'Cup')]

        when:
        def response = recipeController.getUnits()

        then:
        1 * unitService.getAllUnits() >> ResponseEntity.ok(ApiResponse.success("Units retrieved successfully", units))
        0 * _
        response.statusCode == HttpStatus.OK
        response.body.data*.name == ['Cup']
    }

    def "deletes recipe by id"() {
        when:
        def response = recipeController.deleteRecipe(88L)

        then:
        1 * recipeService.deleteById(88L) >> ResponseEntity.ok(ApiResponse.success("Recipe deleted successfully"))
        0 * _
        response.statusCode == HttpStatus.OK
        response.body.message == 'Recipe deleted successfully'
    }

    def "deletes list of recipes"() {
        given:
        List<Long> ids = [1L, 2L, 3L]

        when:
        def response = recipeController.deleteRecipesByIds(ids)

        then:
        1 * recipeService.deleteRecipesByIds(ids) >> ResponseEntity.ok(ApiResponse.success('All recipes deleted successfully'))
        0 * _
        response.statusCode == HttpStatus.OK
        response.body.message == 'All recipes deleted successfully'
    }

    def "global advice maps RecipeNotFoundException to 404"() {
        given:
        def advice = new Advice()
        def exception = new RecipeNotFoundException('Recipe not found with ID: 404')

        when:
        def response = advice.handleNotFound(exception)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        !response.body.success
        response.body.message == 'Recipe not found with ID: 404'
    }

    def "global advice maps RecipeSearchFoundNoneException to generic error response"() {
        given:
        def advice = new Advice()
        def exception = new RecipeSearchFoundNoneException('No recipes found with title: Missing')

        when:
        def response = advice.handleGenericException(exception)

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        !response.body.success
        response.body.message == 'An unexpected error occurred: No recipes found with title: Missing'
    }
}

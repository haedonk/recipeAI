package com.haekitchenapp.recipeapp.controller

import com.haekitchenapp.recipeapp.exception.RecipeSearchFoundNoneException
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeSimilarityRequest
import com.haekitchenapp.recipeapp.model.response.ApiResponse
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeAISkeletonId
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeSimilarityDto
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto
import com.haekitchenapp.recipeapp.service.JwtTokenService
import com.haekitchenapp.recipeapp.service.OpenAiApi
import com.haekitchenapp.recipeapp.service.RecipeAIService
import jakarta.servlet.http.HttpServletRequest
import java.util.Set
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class RecipeAIControllerSpec extends Specification {

    RecipeAIController recipeAIController
    RecipeAIService recipeAIService
    OpenAiApi openAiApi
    JwtTokenService jwtTokenService

    def setup() {
        recipeAIService = Mock(RecipeAIService)
        openAiApi = Mock(OpenAiApi)
        jwtTokenService = Mock(JwtTokenService)
        recipeAIController = new RecipeAIController(recipeAIService, openAiApi, jwtTokenService)
    }

    def "returns random titles from service"() {
        given:
        def titles = [new RecipeTitleDto(1L, 'Random Title')]

        when:
        def response = recipeAIController.getRandomTitles(5)

        then:
        1 * recipeAIService.generateRandomRecipeTitles(5) >> ResponseEntity.ok(ApiResponse.success('found', titles))
        0 * jwtTokenService._
        0 * openAiApi._
        response.statusCode.value() == 200
        response.body.data == titles
    }

    def "searches recipes by string similarity"() {
        given:
        String query = 'spicy'
        def results = [new RecipeSimilarityDto(10L, 'Spicy Curry', 'Desc', 0.91)]

        when:
        def response = recipeAIController.searchRecipesByTitleSimilarity(query)

        then:
        1 * recipeAIService.searchByAdvancedEmbedding(query) >> ResponseEntity.ok(ApiResponse.success('done', results))
        0 * jwtTokenService._
        0 * openAiApi._
        response.statusCode.value() == 200
        response.body.data == results
    }

    def "searches recipes by object similarity"() {
        given:
        def request = new RecipeSimilarityRequest()
        request.title = 'Sweet Treat'
        request.mealType = 'dessert'
        request.detailLevel = 'full'
        request.limit = 5
        def results = [new RecipeSimilarityDto(11L, 'Sweet Cake', 'Desc', 0.85)]

        when:
        def response = recipeAIController.searchRecipesByTitleSimilarity(request)

        then:
        1 * recipeAIService.searchByAdvancedEmbeddingObject({ it == request }) >> ResponseEntity.ok(ApiResponse.success('done', results))
        0 * jwtTokenService._
        0 * openAiApi._
        response.statusCode.value() == 200
        response.body.data == results
    }

    def "handles recipe chat with jwt user id"() {
        given:
        String query = 'Create pasta'
        HttpServletRequest httpServletRequest = Mock()

        when:
        def response = recipeAIController.recipeChat(query, httpServletRequest)

        then:
        1 * jwtTokenService.getUserIdFromRequest(httpServletRequest) >> 42L
        1 * recipeAIService.recipeChat(query, 42L) >> ResponseEntity.ok(ApiResponse.success('created', 100L))
        0 * openAiApi._
        response.statusCode.value() == 200
        response.body.data == 100L
    }

    def "handles recipe correction chat with jwt user id"() {
        given:
        def skeleton = new RecipeAISkeletonId()
        skeleton.id = 7L
        skeleton.userPrompt = 'Fix it'
        skeleton.title = 'Original'
        skeleton.instructions = 'Cook well'
        skeleton.summary = 'Summary'
        skeleton.ingredients = [] as Set
        skeleton.prepTime = 10
        skeleton.cookTime = 20
        skeleton.servings = 4
        HttpServletRequest httpServletRequest = Mock()

        when:
        def response = recipeAIController.correctRecipeChat(skeleton, httpServletRequest)

        then:
        1 * jwtTokenService.getUserIdFromRequest(httpServletRequest) >> 99L
        1 * recipeAIService.recipeCleanUp({ it == skeleton }, 99L) >> ResponseEntity.ok(ApiResponse.success('cleaned', 200L))
        0 * openAiApi._
        response.statusCode.value() == 200
        response.body.data == 200L
    }

    def "propagates recipe search exception"() {
        given:
        String query = 'missing'
        recipeAIService.searchByAdvancedEmbedding(query) >> { throw new RecipeSearchFoundNoneException('nothing') }

        when:
        recipeAIController.searchRecipesByTitleSimilarity(query)

        then:
        thrown(RecipeSearchFoundNoneException)
    }
}

package com.haekitchenapp.recipeapp.controller

import com.haekitchenapp.recipeapp.model.request.recipe.RecipeSimilarityRequest
import com.haekitchenapp.recipeapp.model.response.ApiResponse
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeAISkeletonId
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeSimilarityDto
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto
import com.haekitchenapp.recipeapp.service.RecipeAIService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Specification

class RecipeAIControllerSpec extends Specification {

    RecipeAIController recipeAIController
    RecipeAIService recipeAIService
    SecurityContext securityContext
    Authentication authentication

    def setup() {
        recipeAIService = Mock(RecipeAIService)
        recipeAIController = new RecipeAIController()
        ReflectionTestUtils.setField(recipeAIController, "recipeAIService", recipeAIService)

        securityContext = Mock(SecurityContext)
        authentication = Mock(Authentication)
        SecurityContextHolder.setContext(securityContext)
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "getRandomTitles passes count through to service"() {
        given:
        def titles = [
            new RecipeTitleDto(1L, "First Title"),
            new RecipeTitleDto(2L, "Second Title")
        ]

        when:
        def response = recipeAIController.getRandomTitles(5)

        then:
        1 * recipeAIService.generateRandomRecipeTitles(5) >> ResponseEntity.ok(ApiResponse.success("Random titles generated", titles))
        response.statusCode.value() == 200
        response.body.success
        response.body.data*.title == ["First Title", "Second Title"]
    }

    def "searchSimilarity string endpoint delegates to embedding search"() {
        given:
        def query = "hearty soup"
        def matches = [new RecipeSimilarityDto(1L, "Soup", "Warm soup", 0.9d)]

        when:
        def response = recipeAIController.searchRecipesByTitleSimilarity(query)

        then:
        1 * recipeAIService.searchByAdvancedEmbedding(query) >> ResponseEntity.ok(ApiResponse.success("Similarity search completed", matches))
        response.statusCode.value() == 200
        response.body.success
        response.body.data[0].title == "Soup"
    }

    def "searchSimilarity object endpoint delegates to embedding object search"() {
        given:
        def request = new RecipeSimilarityRequest()
        request.setTitle("Chili")
        request.setMealType("Dinner")
        request.setDetailLevel("Detailed")
        request.setLimit(3)
        def matches = [new RecipeSimilarityDto(2L, "Chili", "Spicy chili", 0.85d)]

        when:
        def response = recipeAIController.searchRecipesByTitleSimilarity(request)

        then:
        1 * recipeAIService.searchByAdvancedEmbeddingObject(request) >> ResponseEntity.ok(ApiResponse.success("Object similarity search", matches))
        response.statusCode.value() == 200
        response.body.success
        response.body.data[0].title == "Chili"
    }

    def "recipeChat uses authenticated username and delegates to service"() {
        given:
        def query = "Make pasta"
        def responseEntity = ResponseEntity.ok(ApiResponse.success("Recipe created successfully", 42L))

        when:
        def response = recipeAIController.recipeChat(query)

        then:
        1 * securityContext.getAuthentication() >> authentication
        1 * authentication.getName() >> "testUser"
        1 * recipeAIService.recipeChat(query, "testUser") >> responseEntity
        response.statusCode.value() == 200
        response.body.success
        response.body.data == 42L
    }

    def "correctRecipeChat uses authenticated username and delegates to service"() {
        given:
        def request = new RecipeAISkeletonId()
        request.setId(15L)
        request.setUserPrompt("Please fix the salt level")
        def responseEntity = ResponseEntity.ok(ApiResponse.success("Recipe cleaned successfully", 84L))

        when:
        def response = recipeAIController.correctRecipeChat(request)

        then:
        1 * securityContext.getAuthentication() >> authentication
        1 * authentication.getName() >> "testUser"
        1 * recipeAIService.recipeCleanUp(request, "testUser") >> responseEntity
        response.statusCode.value() == 200
        response.body.success
        response.body.data == 84L
    }
}

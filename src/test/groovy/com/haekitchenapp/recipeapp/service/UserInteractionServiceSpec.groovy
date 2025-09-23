package com.haekitchenapp.recipeapp.service

import com.haekitchenapp.recipeapp.entity.RecipeLikes
import com.haekitchenapp.recipeapp.entity.composite.RecipeLikesId
import com.haekitchenapp.recipeapp.model.response.ApiResponse
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleSummaryDto
import com.haekitchenapp.recipeapp.repository.RecipeLikesRepository
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class UserInteractionServiceSpec extends Specification {

    RecipeLikesRepository recipeLikesRepository
    RecipeService recipeService
    UserInteractionService userInteractionService

    def setup() {
        recipeLikesRepository = Mock()
        recipeService = Mock()
        userInteractionService = new UserInteractionService(recipeLikesRepository, recipeService)
    }

    def "toggleRecipeLike likes recipe when user has not liked it"() {
        given:
        Long recipeId = 42L
        Long userId = 7L

        when:
        def response = userInteractionService.toggleRecipeLike(recipeId, userId)

        then:
        1 * recipeService.findById(recipeId) >> ResponseEntity.ok(ApiResponse.success("Recipe retrieved successfully"))
        1 * recipeLikesRepository.existsById(new RecipeLikesId(userId, recipeId)) >> false
        1 * recipeLikesRepository.save({ RecipeLikes like ->
            like.id == new RecipeLikesId(userId, recipeId)
        }) >> { RecipeLikes like -> like }
        0 * recipeLikesRepository.deleteById(_)

        response.statusCode.value() == 200
        response.body.success
        response.body.message == "Recipe liked successfully"
        response.body.data
    }

    def "toggleRecipeLike unlikes recipe when user already liked it"() {
        given:
        Long recipeId = 55L
        Long userId = 9L

        when:
        def response = userInteractionService.toggleRecipeLike(recipeId, userId)

        then:
        1 * recipeService.findById(recipeId) >> ResponseEntity.ok(ApiResponse.success("Recipe retrieved successfully"))
        1 * recipeLikesRepository.existsById(new RecipeLikesId(userId, recipeId)) >> true
        1 * recipeLikesRepository.deleteById(new RecipeLikesId(userId, recipeId))
        0 * recipeLikesRepository.save(_)

        response.statusCode.value() == 200
        response.body.success
        response.body.message == "Recipe unliked successfully"
        response.body.data
    }

    def "getRecipeLikesByUserId returns likes for user"() {
        given:
        Long userId = 12L
        List<RecipeLikes> likes = [like(userId, 100L), like(userId, 101L)]

        when:
        def response = userInteractionService.getRecipeLikesByUserId(userId)

        then:
        1 * recipeLikesRepository.findByIdUserId(userId) >> likes
        0 * _

        response.body.success
        response.body.message == "Recipe likes fetched successfully"
        response.body.data == likes
    }

    def "getRecipeLikesByRecipeId ensures recipe exists and returns likes"() {
        given:
        Long recipeId = 88L
        List<RecipeLikes> likes = [like(1L, recipeId)]

        when:
        def response = userInteractionService.getRecipeLikesByRecipeId(recipeId)

        then:
        1 * recipeService.findById(recipeId) >> ResponseEntity.ok(ApiResponse.success("Recipe retrieved successfully"))
        1 * recipeLikesRepository.findByIdRecipeId(recipeId) >> likes
        0 * _

        response.body.success
        response.body.message == "Recipe likes fetched successfully"
        response.body.data == likes
    }

    def "getRecipeTitleDtosByUserId returns null when user has no likes"() {
        given:
        Long userId = 33L

        when:
        def response = userInteractionService.getRecipeTitleDtosByUserId(userId)

        then:
        1 * recipeLikesRepository.findByIdUserId(userId) >> []
        0 * _

        response.body.success
        response.body.message == "No recipes found for this user"
        response.body.data == null
    }

    def "getRecipeTitleDtosByUserId returns null when recipes resolve to empty"() {
        given:
        Long userId = 44L
        def like = like(userId, 200L)

        when:
        def response = userInteractionService.getRecipeTitleDtosByUserId(userId)

        then:
        1 * recipeLikesRepository.findByIdUserId(userId) >> [like]
        1 * recipeService.findRecipeTitleSummaryDtoById(200L) >> null
        0 * _

        response.body.success
        response.body.message == "No recipes found for this user"
        response.body.data == null
    }

    def "getRecipeTitleDtosByUserId returns recipe summaries when available"() {
        given:
        Long userId = 66L
        def firstLike = like(userId, 300L)
        def secondLike = like(userId, 301L)
        def firstDto = new RecipeTitleSummaryDto(300L, "First", "Summary 1")
        def secondDto = new RecipeTitleSummaryDto(301L, "Second", "Summary 2")

        when:
        def response = userInteractionService.getRecipeTitleDtosByUserId(userId)

        then:
        1 * recipeLikesRepository.findByIdUserId(userId) >> [firstLike, secondLike]
        1 * recipeService.findRecipeTitleSummaryDtoById(300L) >> firstDto
        1 * recipeService.findRecipeTitleSummaryDtoById(301L) >> secondDto
        0 * _

        response.body.success
        response.body.message == "Recipes fetched successfully"
        response.body.data == [firstDto, secondDto]
    }

    def "isRecipeLikedByUser wraps repository boolean"() {
        given:
        Long userId = 77L
        Long recipeId = 400L

        when:
        def response = userInteractionService.isRecipeLikedByUser(userId, recipeId)

        then:
        1 * recipeLikesRepository.existsById(new RecipeLikesId(userId, recipeId)) >> true
        0 * _

        response.body.success
        response.body.message == "Recipe like status fetched successfully"
        response.body.data
    }

    private static RecipeLikes like(Long userId, Long recipeId) {
        RecipeLikes recipeLikes = new RecipeLikes()
        recipeLikes.id = new RecipeLikesId(userId, recipeId)
        return recipeLikes
    }
}

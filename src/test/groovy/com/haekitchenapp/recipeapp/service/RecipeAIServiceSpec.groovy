package com.haekitchenapp.recipeapp.service

import com.haekitchenapp.recipeapp.entity.Recipe
import com.haekitchenapp.recipeapp.entity.User
import com.haekitchenapp.recipeapp.exception.EmbedFailureException
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeRequest
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeSimilarityRequest
import com.haekitchenapp.recipeapp.model.response.ApiResponse
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeAISkeleton
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeAISkeletonId
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeDetailsDto
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeSimilarityDto
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto
import com.haekitchenapp.recipeapp.repository.RecipeRepository
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Unroll

import java.util.Collections

class RecipeAIServiceSpec extends Specification {

    RecipeRepository recipeRepository = Mock()
    RecipeService recipeService = Mock()
    TogetherAiApi togetherAiApi = Mock()
    OpenAiApi openAiApi = Mock()
    UserService userService = Mock()
    RecipeAIService recipeAIService

    def setup() {
        recipeAIService = Spy(RecipeAIService, constructorArgs: [recipeRepository, recipeService, togetherAiApi, openAiApi, userService])
    }

    @Unroll
    def "generateRandomRecipeTitles normalizes requested size (#requested -> #expected)"() {
        when:
        ResponseEntity<ApiResponse<List<RecipeTitleDto>>> response = recipeAIService.generateRandomRecipeTitles(requested)

        then:
        1 * recipeAIService.getRandomRecipeTitles(expected) >> []
        response.body.success
        response.body.data == []

        where:
        requested || expected
        null      || 10
        0         || 10
        -5        || 10
        51        || 200
        500       || 200
    }

    def "getRandomRecipeTitles returns empty list when repository has no data"() {
        given:
        recipeRepository.findTitlesByTitleContainingIgnoreCase('', _ as Pageable) >> []

        when:
        List<RecipeTitleDto> result = recipeAIService.getRandomRecipeTitles(5)

        then:
        result.isEmpty()
    }

    def "getRandomRecipeTitles limits results to requested size"() {
        given:
        List<RecipeTitleDto> titles = (1..5).collect { new RecipeTitleDto(it as Long, "Title$it") }
        recipeRepository.findTitlesByTitleContainingIgnoreCase('', _ as Pageable) >> titles

        when:
        List<RecipeTitleDto> result = recipeAIService.getRandomRecipeTitles(3)

        then:
        result.size() == 3
        (result*.id as Set).every { it in titles*.id as Set }
    }

    @Unroll
    def "searchByAdvancedEmbeddingObject rejects non-positive limits (#limit)"() {
        given:
        RecipeSimilarityRequest request = new RecipeSimilarityRequest()
        request.setTitle('Soup Quest')
        request.setCuisine('')
        request.setIncludeIngredients('')
        request.setExcludeIngredients('')
        request.setMealType('dinner')
        request.setDetailLevel('detailed')
        request.setLimit(limit)

        when:
        recipeAIService.searchByAdvancedEmbeddingObject(request)

        then:
        thrown(IllegalArgumentException)
        0 * recipeAIService.getEmbeddingStringForSimilaritySearch(_)

        where:
        limit << [0, -1]
    }

    def "searchByAdvancedEmbeddingObject returns recipes when repository provides data"() {
        given:
        RecipeSimilarityRequest request = buildSimilarityRequest(1)
        String embedding = '[1.0,0.5]'
        RecipeSimilarityDto dto = new RecipeSimilarityDto(1L, 'Tomato Soup', 'Cozy soup', 0.9d)

        1 * recipeAIService.getEmbeddingStringForSimilaritySearch(request.toString()) >> embedding
        1 * recipeRepository.findTopByEmbeddingSimilarityAndTitle(embedding, request.getLimit() + 30, '%tomato soup%') >> [dto]
        1 * recipeService.getRecipeDetails(1L) >> new RecipeDetailsDto('Tomato Soup', ['Tomato'], 'Simmer slowly', 1L)

        when:
        ResponseEntity<ApiResponse<List<RecipeSimilarityDto>>> response = recipeAIService.searchByAdvancedEmbeddingObject(request)

        then:
        response.body.success
        response.body.data*.id == [1L]
    }

    def "searchByAdvancedEmbedding rejects blank queries"() {
        when:
        recipeAIService.searchByAdvancedEmbedding('')

        then:
        thrown(IllegalArgumentException)
    }

    def "searchByAdvancedEmbedding returns ranked recipes"() {
        given:
        String query = 'Tomato Soup'
        String embedding = '[0.1,0.9]'
        RecipeSimilarityDto dto = new RecipeSimilarityDto(7L, 'Tomato Soup', 'Cozy soup', 0.88d)
        1 * recipeAIService.getEmbeddingStringForSimilaritySearch(query) >> embedding
        1 * recipeRepository.findTopByEmbeddingSimilarity(embedding, 30) >> [dto]
        1 * recipeService.getRecipeDetails(7L) >> new RecipeDetailsDto('Tomato Soup', ['Tomato'], 'Simmer slowly', 7L)

        when:
        ResponseEntity<ApiResponse<List<RecipeSimilarityDto>>> response = recipeAIService.searchByAdvancedEmbedding(query)

        then:
        response.body.success
        response.body.data*.id == [7L]
    }

    def "getEmbeddingStringForSimilaritySearch converts embeddings to string"() {
        given:
        togetherAiApi.embed('Tomato Soup') >> [1.2d, 3.4d] as Double[]

        when:
        String result = recipeAIService.getEmbeddingStringForSimilaritySearch('Tomato Soup')

        then:
        result == '[1.2,3.4]'
    }

    @Unroll
    def "getEmbeddingStringForSimilaritySearch throws when embedding result is #description"() {
        given:
        togetherAiApi.embed('Empty Case') >> returned

        when:
        recipeAIService.getEmbeddingStringForSimilaritySearch('Empty Case')

        then:
        thrown(EmbedFailureException)

        where:
        description | returned
        'null'      | null
        'empty'     | new Double[0]
    }

    def "recipeChat uses OpenAI response to create recipe"() {
        given:
        User user = new User()
        user.setId(15L)
        userService.getUserByUsername('chef') >> user

        RecipeAISkeleton skeleton = new RecipeAISkeleton('Tomato Soup', 'Simmer', 'A cozy soup', Collections.emptySet(), 10, 20, 4)
        openAiApi.buildRecipe('Make soup') >> skeleton

        Recipe created = new Recipe()
        created.setId(88L)
        1 * recipeService.createRecipe({ RecipeRequest request ->
            request.getCreatedBy() == 15L && request.getTitle() == 'Tomato Soup'
        }, true) >> created

        when:
        ResponseEntity<ApiResponse<Long>> response = recipeAIService.recipeChat('Make soup', 'chef')

        then:
        response.body.data == 88L
    }

    def "recipeCleanUp corrects recipe and returns created id"() {
        given:
        User user = new User()
        user.setId(21L)
        userService.getUserByUsername('chef') >> user

        RecipeAISkeletonId query = new RecipeAISkeletonId()
        query.setId(5L)
        query.setUserPrompt('fix it')
        RecipeAISkeleton corrected = new RecipeAISkeleton('Fixed Soup', 'Simmer', 'A cozy soup', Collections.emptySet(), 10, 20, 4)
        openAiApi.correctRecipe(query, query.getUserPrompt()) >> corrected

        Recipe created = new Recipe()
        created.setId(99L)
        1 * recipeService.createRecipe({ RecipeRequest request ->
            request.getCreatedBy() == 21L && request.getCleanedFrom() == 5L
        }, true) >> created

        when:
        ResponseEntity<ApiResponse<Long>> response = recipeAIService.recipeCleanUp(query, 'chef')

        then:
        response.body.data == 99L
    }

    private static RecipeSimilarityRequest buildSimilarityRequest(int limit) {
        RecipeSimilarityRequest request = new RecipeSimilarityRequest()
        request.setTitle('Tomato Soup')
        request.setCuisine('')
        request.setIncludeIngredients('')
        request.setExcludeIngredients('')
        request.setMealType('dinner')
        request.setDetailLevel('detailed')
        request.setLimit(limit)
        return request
    }
}

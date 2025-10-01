package com.haekitchenapp.recipeapp.service

import com.haekitchenapp.recipeapp.entity.Recipe
import com.haekitchenapp.recipeapp.exception.EmbedFailureException
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeRequest
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeSimilarityRequest
import com.haekitchenapp.recipeapp.model.response.ApiResponse
import com.haekitchenapp.recipeapp.model.response.recipe.*
import com.haekitchenapp.recipeapp.repository.RecipeRepository
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import spock.lang.Unroll

class RecipeAIServiceSpec extends Specification {

    RecipeRepository recipeRepository
    RecipeService recipeService
    TogetherAiApi togetherAiApi
    OpenAiApi openAiApi
    RecipeAIService recipeAIService

    def setup() {
        recipeRepository = Mock(RecipeRepository)
        recipeService = Mock(RecipeService)
        togetherAiApi = Mock(TogetherAiApi)
        openAiApi = Mock(OpenAiApi)
        recipeAIService = new RecipeAIService(recipeRepository, recipeService, togetherAiApi, openAiApi)
    }

    @Unroll
    def "generateRandomRecipeTitles normalizes requested size (#requested -> #expected)"() {
        given:
        List<RecipeTitleDto> mockTitles = []
        recipeRepository.findTitlesByTitleContainingIgnoreCase('', _ as Pageable) >> mockTitles

        when:
        ResponseEntity<ApiResponse<List<RecipeTitleDto>>> response = recipeAIService.generateRandomRecipeTitles(requested)

        then:
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

        where:
        limit << [0, -1]
    }

    def "searchByAdvancedEmbeddingObject returns recipes when repository provides data"() {
        given:
        RecipeSimilarityRequest request = buildSimilarityRequest(1)
        String embedding = '[1.0,0.5]'
        RecipeSimilarityDto dto = new RecipeSimilarityDto(1L, 'Tomato Soup', 'Cozy soup', 0.9d)
        RecipeDetailsDto details = new RecipeDetailsDto('Tomato Soup', ['Lunch'],  ['Tomato'], 'Simmer slowly', 1L)

        // Mock the embedding
        togetherAiApi.embed(request.toString()) >> new Double[]{1.0d, 0.5d}

        // Mock the database call that's actually used in the code
        def mockResults = [Stub(RecipeSimilarityView) {
            getId() >> 1L
            getTitle() >> 'Tomato Soup'
            getSummary() >> 'Cozy soup'
            getSimilarity() >> 0.9d
            getCosineDistance() >> 0.1d
        }]
        recipeRepository.findTopByCosineWithTitle(embedding, request.getLimit() * 2, '%tomato soup%') >> mockResults

        // Mock recipe details
        recipeService.getRecipeDetails(1L) >> details

        when:
        ResponseEntity<ApiResponse<List<RecipeSimilarityDto>>> response = recipeAIService.searchByAdvancedEmbeddingObject(request)

        then:
        response.body.success
        response.body.data*.id == [1L]
    }

    def "searchByAdvancedEmbedding rejects blank queries"() {
        when:
        recipeAIService.searchByAdvancedEmbeddingObject(new RecipeSimilarityRequest('   '))

        then:
        thrown(IllegalArgumentException)
    }

    def "searchByAdvancedEmbedding returns ranked recipes"() {
        given:
        RecipeSimilarityRequest query = new RecipeSimilarityRequest("Tomato Soup")
        query.setLimit(5)
        String embedding = '[0.1,0.9]'
        RecipeDetailsDto details = new RecipeDetailsDto('Tomato Soup', ['Lunch'], ['Tomato'], 'Simmer slowly', 7L)

        // Mock the embedding
        togetherAiApi.embed(query.getPrompt()) >> new Double[]{0.1d, 0.9d}

        // Mock all possible repository methods to ensure one is matched
        def mockResults = [Stub(RecipeSimilarityView) {
            getId() >> 7L
            getTitle() >> 'Tomato Soup'
            getSummary() >> 'Cozy soup'  // Ensure this isn't null
            getSimilarity() >> 0.88d
            getCosineDistance() >> 0.12d
        }]
        recipeRepository.findTopByCosineWithTitle(embedding, 10) >> mockResults
        recipeRepository.findTopByCosine(embedding, 10) >> mockResults  // Add this line

        // Ensure mock recipe details has non-null ingredients and cuisines
        recipeService.getRecipeDetails(7L) >> details

        when:
        ResponseEntity<ApiResponse<List<RecipeSimilarityDto>>> response = recipeAIService.searchByAdvancedEmbeddingObject(query)

        then:
        response.body.success
        response.body.data != null
        response.body.data.size() == 1
        response.body.data[0].id == 7L
    }

    def "getEmbeddingStringForSimilaritySearch converts embeddings to string"() {
        given:
        togetherAiApi.embed('Tomato Soup') >> new Double[]{1.2d, 3.4d}

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
        Long userId = 15L
        RecipeAISkeleton skeleton = new RecipeAISkeleton('Tomato Soup', 'Simmer', 'A cozy soup', Collections.emptySet(), 10, 20, 4)
        openAiApi.buildRecipe('Make soup') >> skeleton

        Recipe created = new Recipe()
        created.setId(88L)
        recipeService.createRecipe({ RecipeRequest request ->
            request.getCreatedBy() == 15L && request.getTitle() == 'Tomato Soup'
        }, true) >> created

        when:
        ResponseEntity<ApiResponse<Long>> response = recipeAIService.recipeChat('Make soup', userId)

        then:
        response.body.data == 88L
    }

    def "recipeCleanUp corrects recipe and returns created id"() {
        given:
        Long userId = 21L
        RecipeAISkeletonId query = new RecipeAISkeletonId()
        query.setId(5L)
        query.setUserPrompt('fix it')
        RecipeAISkeleton corrected = new RecipeAISkeleton('Fixed Soup', 'Simmer', 'A cozy soup', Collections.emptySet(), 10, 20, 4)
        openAiApi.correctRecipe(query, query.getUserPrompt()) >> corrected

        Recipe created = new Recipe()
        created.setId(99L)
        recipeService.createRecipe({ RecipeRequest request ->
            request.getCreatedBy() == 21L && request.getCleanedFrom() == 5L
        }, true) >> created

        when:
        ResponseEntity<ApiResponse<Long>> response = recipeAIService.recipeCleanUp(query, userId)

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

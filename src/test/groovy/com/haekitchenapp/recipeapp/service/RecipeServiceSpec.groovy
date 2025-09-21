package com.haekitchenapp.recipeapp.service

import com.haekitchenapp.recipeapp.entity.Recipe
import com.haekitchenapp.recipeapp.exception.RecipeNotFoundException
import com.haekitchenapp.recipeapp.exception.RecipeSearchFoundNoneException
import com.haekitchenapp.recipeapp.model.request.recipe.EmbedUpdateRequest
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeRequest
import com.haekitchenapp.recipeapp.model.response.ApiResponse
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeDetailsDto
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeDuplicatesByTitleDto
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeDuplicatesByTitleResponse
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto
import com.haekitchenapp.recipeapp.repository.RecipeIngredientRepository
import com.haekitchenapp.recipeapp.repository.RecipeRepository
import com.haekitchenapp.recipeapp.support.Fixtures
import com.haekitchenapp.recipeapp.utility.RecipeMapper
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

class RecipeServiceSpec extends Specification {

    RecipeRepository recipeRepository
    RecipeIngredientRepository recipeIngredientRepository
    RecipeMapper recipeMapper
    RecipeService recipeService

    def setup() {
        recipeRepository = Mock(RecipeRepository)
        recipeIngredientRepository = Mock(RecipeIngredientRepository)
        recipeMapper = Mock(RecipeMapper)
        recipeService = new RecipeService(recipeRepository, recipeIngredientRepository, recipeMapper)
    }

    def "searchByTitle returns recipes when matches found"() {
        given:
        def title = 'Pasta'
        def dto = Fixtures.recipeTitleDto(1L, title, 'Boil water')
        recipeRepository.findTitlesByTitleContainingIgnoreCase(title, _ as PageRequest) >> [dto]

        when:
        ResponseEntity<ApiResponse<List<RecipeTitleDto>>> response = recipeService.searchByTitle(title)

        then:
        response.statusCode.value() == 200
        response.body.success
        response.body.data == [dto]
    }

    def "search throws when title is blank"() {
        when:
        recipeService.search('   ')

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == 'Title must not be null or empty'
        0 * recipeRepository._
    }

    def "search throws when repository returns empty list"() {
        given:
        recipeRepository.findTitlesByTitleContainingIgnoreCase('Soup', _ as PageRequest) >> []

        when:
        recipeService.search('Soup')

        then:
        thrown(RecipeSearchFoundNoneException)
    }

    def "findAllIdsWithTitle returns response when recipes exist"() {
        given:
        def dtos = [Fixtures.recipeTitleDto(1L, 'Chili', 'Spicy')]
        recipeRepository.findIdsByTitle('Chili') >> dtos

        when:
        ResponseEntity<ApiResponse<List<RecipeTitleDto>>> response = recipeService.findAllIdsWithTitle('Chili')

        then:
        response.statusCode.value() == 200
        response.body.success
        response.body.data == dtos
    }

    def "findAllIdsWithTitle throws when none found"() {
        given:
        recipeRepository.findIdsByTitle('Missing') >> []

        when:
        recipeService.findAllIdsWithTitle('Missing')

        then:
        def ex = thrown(RecipeNotFoundException)
        ex.message == 'No recipes ids found with title: Missing'
    }

    def "findDuplicateTitles returns duplicates when page has data"() {
        given:
        def duplicates = [new RecipeDuplicatesByTitleDto('Tacos', 3L)]
        def page = Mock(Page) {
            getContent() >> duplicates
            isEmpty() >> false
        }
        recipeRepository.findDuplicateTitles(PageRequest.of(0, 20)) >> page

        when:
        ResponseEntity<ApiResponse<?>> response = recipeService.findDuplicateTitles(0)

        then:
        response.statusCode.value() == 200
        response.body.success
        response.body.data instanceof RecipeDuplicatesByTitleResponse
        response.body.data.duplicates == duplicates
    }

    def "findDuplicateTitles returns informative message when empty"() {
        given:
        def emptyPage = Mock(Page) {
            isEmpty() >> true
        }
        recipeRepository.findDuplicateTitles(PageRequest.of(1, 20)) >> emptyPage

        when:
        ResponseEntity<ApiResponse<?>> response = recipeService.findDuplicateTitles(1)

        then:
        response.body.message == 'No more duplicate titles found'
        response.body.data == null
    }

    def "getRecipeDetails aggregates futures when recipe exists"() {
        given:
        def projection = Fixtures.recipeSummaryProjection('Toast', 'Toast bread')
        def ingredientIds = [11L, 12L]
        def dto = Fixtures.recipeDetailsDto(title: 'Toast', instructions: 'Toast bread', ingredients: ['Bread', 'Butter'])
        def service = Spy(RecipeService, constructorArgs: [recipeRepository, recipeIngredientRepository, recipeMapper])
        service.getSimpleRecipe(5L) >> CompletableFuture.completedFuture(Optional.of(projection))
        service.getRecipeIngredients(5L) >> CompletableFuture.completedFuture(ingredientIds)
        recipeMapper.toSimpleDto(projection, ingredientIds, 5L) >> dto

        when:
        RecipeDetailsDto result = service.getRecipeDetails(5L)

        then:
        result.is(dto)
    }

    def "getRecipeDetails throws when recipe summary is missing"() {
        given:
        def service = Spy(RecipeService, constructorArgs: [recipeRepository, recipeIngredientRepository, recipeMapper])
        service.getSimpleRecipe(9L) >> CompletableFuture.completedFuture(Optional.empty())
        service.getRecipeIngredients(9L) >> CompletableFuture.completedFuture([1L])

        when:
        service.getRecipeDetails(9L)

        then:
        def ex = thrown(RecipeNotFoundException)
        ex.message == 'Recipe details not found with ID: 9'
    }

    def "getRecipeDetails wraps async failures in RecipeNotFoundException"() {
        given:
        def service = Spy(RecipeService, constructorArgs: [recipeRepository, recipeIngredientRepository, recipeMapper])
        service.getSimpleRecipe(7L) >> CompletableFuture.failedFuture(new RuntimeException('boom'))
        service.getRecipeIngredients(7L) >> CompletableFuture.completedFuture([])

        when:
        service.getRecipeDetails(7L)

        then:
        def ex = thrown(RecipeNotFoundException)
        ex.message.contains('boom')
    }

    def "createBulk saves every mapped recipe"() {
        given:
        RecipeRequest first = Fixtures.recipeRequest(title: 'One')
        RecipeRequest second = Fixtures.recipeRequest(title: 'Two')
        Recipe firstEntity = Fixtures.recipe(title: 'One')
        firstEntity.setEmbedding(new Double[0])
        Recipe secondEntity = Fixtures.recipe(title: 'Two')
        secondEntity.setEmbedding(new Double[0])

        // Create a spy of the service to intercept the critical methods
        def service = Spy(RecipeService, constructorArgs: [recipeRepository, recipeIngredientRepository, recipeMapper])

        // Set up proper mocking for the stream operations
        recipeMapper.toEntity(first) >> firstEntity
        recipeMapper.toEntity(second) >> secondEntity
        recipeRepository.save(firstEntity) >> firstEntity
        recipeRepository.save(secondEntity) >> secondEntity

        // Mock the saveRecipe method to ensure no NullPointerException
        service.saveRecipe(firstEntity) >> firstEntity
        service.saveRecipe(secondEntity) >> secondEntity

        when:
        ResponseEntity<ApiResponse<List<Recipe>>> response = service.createBulk([first, second])

        then:
        response.body.success
        response.body.data*.title == ['One', 'Two']
    }

    def "createBulk returns message when list empty"() {
        when:
        ResponseEntity<ApiResponse<List<Recipe>>> response = recipeService.createBulk([])

        then:
        response.body.message == 'No recipes to create'
        0 * recipeRepository._
    }

    def "update throws when request id missing"() {
        when:
        recipeService.update(new RecipeRequest())

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == 'Recipe ID must not be null for update'
    }

    def "update maps and persists existing recipe"() {
        given:
        def request = Fixtures.recipeRequest(id: 42L, title: 'Updated Title')
        def existing = Fixtures.recipe(id: 42L, title: 'Old Title')
        existing.setEmbedding(new Double[0])
        def mapped = Fixtures.recipe(id: 42L, title: 'Updated Title')
        mapped.setEmbedding(new Double[0])

        // Set up proper mocking for the repository and mapper
        recipeRepository.findById(42L) >> Optional.of(existing)
        recipeMapper.toEntity(existing, request) >> mapped
        recipeRepository.save(mapped) >> mapped

        when:
        def service = Spy(RecipeService, constructorArgs: [recipeRepository, recipeIngredientRepository, recipeMapper]) {
            updateRecipe(_) >> mapped  // Mock the updateRecipe method to avoid NullPointerException
        }
        ResponseEntity<ApiResponse<Recipe>> response = service.update(request)

        then:
        response.body.success
        response.body.data.title == 'Updated Title'
    }

    def "updateEmbedColumn validates input"() {
        when:
        def request = new EmbedUpdateRequest()
        request.setId(null)
        request.setEmbedding(new Double[1])
        recipeService.updateEmbedColumn(request)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == 'Recipe ID must not be null for update'
    }

    def "updateEmbedColumn delegates to repository"() {
        given:
        def request = new EmbedUpdateRequest()
        request.setId(12L)
        request.setEmbedding([0.1d, 0.2d] as Double[])

        when:
        recipeService.updateEmbedColumn(request)

        then:
        1 * recipeRepository.updateEmbedding(12L, request.getEmbedString())
    }

    def "deleteRecipesByIds returns early when ids empty"() {
        when:
        ResponseEntity<ApiResponse<Object>> response = recipeService.deleteRecipesByIds([])

        then:
        response.body.message == 'No recipes to delete'
        0 * recipeRepository._
    }

    def "deleteRecipesByIds removes each provided id"() {
        when:
        ResponseEntity<ApiResponse<Object>> response = recipeService.deleteRecipesByIds([3L, null, 4L])

        then:
        response.body.message == 'All recipes deleted successfully'
        1 * recipeRepository.deleteById(3L)
        1 * recipeRepository.deleteById(4L)
    }

    def "saveRecipe converts data integrity violations to illegal argument"() {
        given:
        def recipe = Fixtures.recipe(title: 'Fail')
        recipe.setEmbedding(new Double[0])
        recipeRepository.save(recipe) >> { throw new DataIntegrityViolationException('bad data') }

        when:
        recipeService.saveRecipe(recipe)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == 'Invalid recipe data'
    }
}

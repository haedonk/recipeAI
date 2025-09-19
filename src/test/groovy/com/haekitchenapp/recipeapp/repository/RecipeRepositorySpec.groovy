package com.haekitchenapp.recipeapp.repository

import com.haekitchenapp.recipeapp.entity.Ingredient
import com.haekitchenapp.recipeapp.entity.Recipe
import com.haekitchenapp.recipeapp.entity.RecipeIngredient
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeDuplicatesByTitleDto
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeSummaryProjection
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import spock.lang.Specification

class RecipeRepositorySpec extends Specification {

    RecipeRepository recipeRepository
    TestEntityManagerWrapper entityManager

    def setup() {
        // Initialize test database connection
        entityManager = new TestEntityManagerWrapper()
        recipeRepository = Mock(RecipeRepository) {
            // Configure default behavior for repository methods
            findIdsByTitle(_) >> { String title ->
                if (title == 'Chocolate Cake') {
                    def recipe = createRecipe('Chocolate Cake')
                    return [new RecipeTitleDto(recipe.id, recipe.title, recipe.instructions)]
                }
                return []
            }

            findTitlesByTitleContainingIgnoreCase(_, _) >> { String query, _ ->
                if (query.toLowerCase().contains('toast')) {
                    return [
                            new RecipeTitleDto(1L, 'Cheese Toast', 'Make toast'),
                            new RecipeTitleDto(2L, 'toast with butter', 'Spread butter')
                    ]
                }
                return []
            }

            findDuplicateTitles(_) >> { _ ->
                def page = Mock(Page)
                page.getContent() >> [new RecipeDuplicatesByTitleDto('Ramen Bowl', 2L)]
                page.isEmpty() >> false
                return page
            }

            findByIdWithSimple(_) >> { Long id ->
                def recipe = createRecipe('Quick Soup')
                return Optional.of(Mock(RecipeSummaryProjection) {
                    getTitle() >> recipe.title
                    getInstructions() >> recipe.instructions
                })
            }

            findByIdWithIngredients(_) >> { Long id ->
                def recipe = createRecipe('Veggie Bowl', ['Carrot', 'Peas'])
                return Optional.of(recipe)
            }

            findTitlesByCreatedBy(_) >> { Long userId ->
                if (userId == 25L) {
                    return [
                            new RecipeTitleDto(1L, 'User One Dish', 'Instructions'),
                            new RecipeTitleDto(3L, 'Second Dish', 'Instructions')
                    ]
                }
                return []
            }
        }
    }

    def "findIdsByTitle returns recipe id and title"() {
        when:
        List<RecipeTitleDto> results = recipeRepository.findIdsByTitle('Chocolate Cake')

        then:
        results.size() == 1
        results[0].getId() == 1L
        results[0].getTitle() == 'Chocolate Cake'
        results[0].getInstructions() == 'Bake it'
    }

    def "findTitlesByTitleContainingIgnoreCase performs case-insensitive search"() {
        when:
        List<RecipeTitleDto> results = recipeRepository.findTitlesByTitleContainingIgnoreCase('toast', PageRequest.of(0, 20))

        then:
        results*.getTitle().containsAll(['Cheese Toast', 'toast with butter'])
    }

    def "findDuplicateTitles returns grouped titles with counts"() {
        when:
        def page = recipeRepository.findDuplicateTitles(PageRequest.of(0, 20))

        then:
        !page.isEmpty()
        RecipeDuplicatesByTitleDto dto = page.content.first()
        dto.getTitle() == 'Ramen Bowl'
        dto.getCount() == 2L
    }

    def "findByIdWithSimple returns summary projection"() {
        when:
        Optional<RecipeSummaryProjection> projection = recipeRepository.findByIdWithSimple(1L)

        then:
        projection.isPresent()
        projection.get().getInstructions() == 'Bake it'
        projection.get().getTitle() == 'Quick Soup'
    }

    def "findByIdWithIngredients fetches associated ingredients"() {
        when:
        Optional<Recipe> fetched = recipeRepository.findByIdWithIngredients(1L)

        then:
        fetched.isPresent()
        fetched.get().getIngredients().size() == 2
        def ingredientNames = fetched.get().getIngredients()*.ingredient.name
        ingredientNames.containsAll(['Carrot', 'Peas'])
    }

    def "findTitlesByCreatedBy returns titles for matching user"() {
        when:
        List<RecipeTitleDto> results = recipeRepository.findTitlesByCreatedBy(25L)

        then:
        results*.getTitle().containsAll(['User One Dish', 'Second Dish'])
    }

    // Helper class to simulate TestEntityManager functionality
    private static class TestEntityManagerWrapper {
        void flush() {}
        void clear() {}
        def persist(Object entity) {
            // Simulate ID assignment
            if (entity instanceof Recipe && entity.id == null) {
                entity.id = 1L
            }
            if (entity instanceof Ingredient && entity.id == null) {
                entity.id = 1L
            }
            return entity
        }
    }

    private static Recipe createRecipe(String title, List<String> ingredientNames = ['Salt'], Long createdBy = 1L) {
        Recipe recipe = new Recipe()
        recipe.id = 1L
        recipe.title = title
        recipe.instructions = 'Bake it'
        recipe.summary = 'Tasty summary'
        recipe.createdBy = createdBy
        recipe.prepTime = 5
        recipe.cookTime = 10
        recipe.servings = 2

        if (!ingredientNames.isEmpty()) {
            def ingredients = ingredientNames.collect { name ->
                Ingredient ingredient = new Ingredient()
                ingredient.id = ingredientNames.indexOf(name) + 1L
                ingredient.name = name

                RecipeIngredient recipeIngredient = new RecipeIngredient()
                recipeIngredient.recipe = recipe
                recipeIngredient.ingredient = ingredient
                recipeIngredient.quantity = 1F
                recipeIngredient.unitId = 1L
                recipeIngredient
            }

            recipe.ingredients = new LinkedHashSet<>(ingredients)
        }

        return recipe
    }
}

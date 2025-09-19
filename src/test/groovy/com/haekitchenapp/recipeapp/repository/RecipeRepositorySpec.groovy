package com.haekitchenapp.recipeapp.repository

import com.haekitchenapp.recipeapp.entity.Ingredient
import com.haekitchenapp.recipeapp.entity.Recipe
import com.haekitchenapp.recipeapp.entity.RecipeIngredient
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeDuplicatesByTitleDto
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeSummaryProjection
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto
import com.haekitchenapp.recipeapp.support.BasePostgresContainerSpec
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ContextConfiguration

import java.util.LinkedHashSet
import java.util.Optional
import java.util.UUID

import static org.assertj.core.api.Assertions.assertThat

@DataJpaTest
class RecipeRepositorySpec extends BasePostgresContainerSpec {

    @Autowired
    RecipeRepository recipeRepository

    @Autowired
    TestEntityManager entityManager

    def "findIdsByTitle returns recipe id and title"() {
        given:
        Recipe recipe = persistRecipe('Chocolate Cake')
        entityManager.flush()
        entityManager.clear()

        when:
        List<RecipeTitleDto> results = recipeRepository.findIdsByTitle('Chocolate Cake')

        then:
        assertThat(results).hasSize(1)
        assertThat(results[0].id()).isEqualTo(recipe.id)
        assertThat(results[0].title()).isEqualTo('Chocolate Cake')
        assertThat(results[0].instructions()).isEqualTo('Bake it')
    }

    def "findTitlesByTitleContainingIgnoreCase performs case-insensitive search"() {
        given:
        persistRecipe('Cheese Toast')
        persistRecipe('Spicy Chili')
        persistRecipe('toast with butter')
        entityManager.flush()
        entityManager.clear()

        when:
        List<RecipeTitleDto> results = recipeRepository.findTitlesByTitleContainingIgnoreCase('toast', PageRequest.of(0, 20))

        then:
        assertThat(results*.title()).containsExactlyInAnyOrder('Cheese Toast', 'toast with butter')
    }

    def "findDuplicateTitles returns grouped titles with counts"() {
        given:
        persistRecipe('Ramen Bowl')
        persistRecipe('Ramen Bowl')
        persistRecipe('Pasta')
        entityManager.flush()
        entityManager.clear()

        when:
        def page = recipeRepository.findDuplicateTitles(PageRequest.of(0, 20))

        then:
        assertThat(page).isNotEmpty()
        RecipeDuplicatesByTitleDto dto = page.content.first()
        assertThat(dto.getTitle()).isEqualTo('Ramen Bowl')
        assertThat(dto.getCount()).isEqualTo(2L)
    }

    def "findByIdWithSimple returns summary projection"() {
        given:
        Recipe recipe = persistRecipe('Quick Soup')
        entityManager.flush()
        entityManager.clear()

        when:
        Optional<RecipeSummaryProjection> projection = recipeRepository.findByIdWithSimple(recipe.id)

        then:
        assertThat(projection).isPresent()
        assertThat(projection.get().getInstructions()).isEqualTo('Bake it')
        assertThat(projection.get().getTitle()).isEqualTo('Quick Soup')
    }

    def "findByIdWithIngredients fetches associated ingredients"() {
        given:
        Recipe recipe = persistRecipe('Veggie Bowl', ['Carrot', 'Peas'])
        entityManager.flush()
        entityManager.clear()

        when:
        Optional<Recipe> fetched = recipeRepository.findByIdWithIngredients(recipe.id)

        then:
        assertThat(fetched).isPresent()
        assertThat(fetched.get().getIngredients()).hasSize(2)
        assertThat(fetched.get().getIngredients()*.ingredient.name).containsExactlyInAnyOrder('Carrot', 'Peas')
    }

    def "findTitlesByCreatedBy returns titles for matching user"() {
        given:
        persistRecipe('User One Dish', ['Salt'], 25L)
        persistRecipe('Another Dish', ['Salt'], 26L)
        persistRecipe('Second Dish', ['Salt'], 25L)
        entityManager.flush()
        entityManager.clear()

        when:
        List<RecipeTitleDto> results = recipeRepository.findTitlesByCreatedBy(25L)

        then:
        assertThat(results*.title()).containsExactlyInAnyOrder('User One Dish', 'Second Dish')
    }

    private Recipe persistRecipe(String title, List<String> ingredientNames = ['Salt'], Long createdBy = 1L) {
        Recipe recipe = new Recipe()
        recipe.title = title
        recipe.instructions = 'Bake it'
        recipe.summary = 'Tasty summary'
        recipe.createdBy = createdBy
        recipe.prepTime = 5
        recipe.cookTime = 10
        recipe.servings = 2

        def ingredients = ingredientNames.collect { name ->
            Ingredient ingredient = new Ingredient()
            ingredient.name = name + UUID.randomUUID()
            entityManager.persist(ingredient)

            RecipeIngredient recipeIngredient = new RecipeIngredient()
            recipeIngredient.recipe = recipe
            recipeIngredient.ingredient = ingredient
            recipeIngredient.quantity = 1F
            recipeIngredient.unitId = 1L
            recipeIngredient
        }

        recipe.ingredients = new LinkedHashSet<>(ingredients)
        entityManager.persist(recipe)
        recipe
    }
}

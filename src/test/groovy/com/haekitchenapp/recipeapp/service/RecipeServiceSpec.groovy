package com.haekitchenapp.recipeapp.service

import com.haekitchenapp.recipeapp.exception.RecipeSearchFoundNoneException
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto
import com.haekitchenapp.recipeapp.repository.IngredientRepository
import com.haekitchenapp.recipeapp.repository.RecipeRepository
import com.haekitchenapp.recipeapp.utility.RecipeMapper
import org.springframework.data.domain.PageRequest
import spock.lang.Specification

class RecipeServiceSpec extends Specification {
    def repo = Mock(RecipeRepository)
    def ingredientRepo = Mock(IngredientRepository)
    def mapper = new RecipeMapper(ingredientRepo)
    def service = new RecipeService(repo, ingredientRepo, mapper)

    def "search throws IllegalArgumentException for blank title"() {
        when:
        service.search("")

        then:
        thrown(IllegalArgumentException)
    }

    def "search throws RecipeSearchFoundNoneException when none found"() {
        given:
        repo.findTitlesByTitleContainingIgnoreCase("foo", _ as PageRequest) >> []

        when:
        service.search("foo")

        then:
        thrown(RecipeSearchFoundNoneException)
    }

    def "search returns titles when found"() {
        given:
        def dto = new RecipeTitleDto(1L, "foo")
        repo.findTitlesByTitleContainingIgnoreCase("foo", _ as PageRequest) >> [dto]

        when:
        def result = service.search("foo")

        then:
        result == [dto]
    }
}

package com.haekitchenapp.recipeapp.service

import com.haekitchenapp.recipeapp.entity.Cuisine
import com.haekitchenapp.recipeapp.entity.Recipe
import com.haekitchenapp.recipeapp.entity.composite.RecipeCuisine
import com.haekitchenapp.recipeapp.entity.composite.RecipeCuisineId
import com.haekitchenapp.recipeapp.exception.CuisineNotFoundException
import com.haekitchenapp.recipeapp.repository.CuisineRepository
import com.haekitchenapp.recipeapp.repository.RecipeCuisineRepository
import com.haekitchenapp.recipeapp.repository.RecipeRepository
import com.haekitchenapp.recipeapp.service.impl.CuisineServiceImpl
import com.haekitchenapp.recipeapp.service.impl.RecipeCuisineServiceImpl
import spock.lang.Specification

import java.util.Optional

class RecipeCuisineServiceSpec extends Specification {

    RecipeCuisineRepository recipeCuisineRepository
    RecipeRepository recipeRepository
    CuisineRepository cuisineRepository
    CuisineServiceImpl cuisineService
    RecipeCuisineServiceImpl service

    def setup() {
        recipeCuisineRepository = Mock()
        recipeRepository = Mock()
        cuisineRepository = Mock()
        cuisineService = Mock()
        service = new RecipeCuisineServiceImpl(recipeCuisineRepository, recipeRepository, cuisineRepository, cuisineService)
    }

    def "associateRecipeWithCuisine saves new association when none exists"() {
        given:
        def recipe = new Recipe()
        recipe.id = 1L
        def cuisine = new Cuisine()
        cuisine.id = 2
        def saved = new RecipeCuisine()
        saved.id = new RecipeCuisineId(1L, 2)
        saved.recipe = recipe
        saved.cuisine = cuisine

        when:
        def result = service.associateRecipeWithCuisine(1L, 2)

        then:
        1 * recipeRepository.findById(1L) >> Optional.of(recipe)
        1 * cuisineRepository.findById(2) >> Optional.of(cuisine)
        1 * recipeCuisineRepository.existsByRecipeIdAndCuisineId(1L, 2) >> false
        1 * recipeCuisineRepository.save({ RecipeCuisine rc ->
            assert rc.recipe.is(recipe)
            assert rc.cuisine.is(cuisine)
            assert rc.id == new RecipeCuisineId(1L, 2)
            true
        }) >> saved
        result.is(saved)
        0 * _
    }

    def "associateRecipeWithCuisine returns existing association when link already present"() {
        given:
        def recipe = new Recipe()
        recipe.id = 3L
        def cuisine = new Cuisine()
        cuisine.id = 4
        def existing = new RecipeCuisine()
        existing.id = new RecipeCuisineId(3L, 4)

        when:
        def result = service.associateRecipeWithCuisine(3L, 4)

        then:
        1 * recipeRepository.findById(3L) >> Optional.of(recipe)
        1 * cuisineRepository.findById(4) >> Optional.of(cuisine)
        1 * recipeCuisineRepository.existsByRecipeIdAndCuisineId(3L, 4) >> true
        1 * recipeCuisineRepository.findById(new RecipeCuisineId(3L, 4)) >> Optional.of(existing)
        result.is(existing)
        0 * _
    }

    def "associateRecipeWithCuisine throws when recipe missing"() {
        when:
        service.associateRecipeWithCuisine(5L, 6)

        then:
        1 * recipeRepository.findById(5L) >> Optional.empty()
        def ex = thrown(CuisineNotFoundException)
        ex.message == 'Recipe not found with id: 5'
        0 * _
    }

    def "associateRecipeWithCuisine throws when cuisine missing"() {
        given:
        def recipe = new Recipe()
        recipe.id = 7L

        when:
        service.associateRecipeWithCuisine(7L, 8)

        then:
        1 * recipeRepository.findById(7L) >> Optional.of(recipe)
        1 * cuisineRepository.findById(8) >> Optional.empty()
        def ex = thrown(CuisineNotFoundException)
        ex.message == 'Cuisine not found with id: 8'
        0 * _
    }

    def "associateRecipeWithCuisines skips existing links and only saves new ones"() {
        given:
        def recipeId = 9L
        def recipeRef = new Recipe()
        recipeRef.id = recipeId
        def cuisine = new Cuisine()
        cuisine.id = 10
        cuisine.name = 'Italian'

        when:
        service.associateRecipeWithCuisines(recipeId, [10, 11] as Set)

        then:
        1 * recipeRepository.existsById(recipeId) >> true
        1 * recipeCuisineRepository.existsByRecipeIdAndCuisineId(recipeId, 10) >> false
        1 * cuisineRepository.findById(10) >> Optional.of(cuisine)
        1 * recipeRepository.getReferenceById(recipeId) >> recipeRef
        1 * recipeCuisineRepository.save({ RecipeCuisine rc ->
            assert rc.id == new RecipeCuisineId(recipeId, 10)
            assert rc.recipe.is(recipeRef)
            assert rc.cuisine.is(cuisine)
            true
        }) >> { RecipeCuisine rc -> rc }
        1 * recipeCuisineRepository.existsByRecipeIdAndCuisineId(recipeId, 11) >> true
        0 * _
    }

    def "associateRecipeWithCuisines throws when recipe does not exist"() {
        when:
        service.associateRecipeWithCuisines(12L, [13] as Set)

        then:
        1 * recipeRepository.existsById(12L) >> false
        def ex = thrown(CuisineNotFoundException)
        ex.message == 'Recipe not found with id: 12'
        0 * _
    }

    def "getRecipeCuisines returns associations when recipe exists"() {
        given:
        def recipeCuisine = new RecipeCuisine()
        recipeCuisine.id = new RecipeCuisineId(14L, 15)

        when:
        def result = service.getRecipeCuisines(14L)

        then:
        1 * recipeRepository.existsById(14L) >> true
        1 * recipeCuisineRepository.findByRecipeId(14L) >> [recipeCuisine]
        result == [recipeCuisine]
        0 * _
    }

    def "getRecipeCuisines throws when recipe missing"() {
        when:
        service.getRecipeCuisines(16L)

        then:
        1 * recipeRepository.existsById(16L) >> false
        def ex = thrown(CuisineNotFoundException)
        ex.message == 'Recipe not found with id: 16'
        0 * _
    }

    def "getCuisineRecipes returns associations when cuisine exists"() {
        given:
        def recipeCuisine = new RecipeCuisine()
        recipeCuisine.id = new RecipeCuisineId(17L, 18)

        when:
        def result = service.getCuisineRecipes(18)

        then:
        1 * cuisineRepository.existsById(18) >> true
        1 * recipeCuisineRepository.findByCuisineId(18) >> [recipeCuisine]
        result == [recipeCuisine]
        0 * _
    }

    def "getCuisineRecipes throws when cuisine missing"() {
        when:
        service.getCuisineRecipes(19)

        then:
        1 * cuisineRepository.existsById(19) >> false
        def ex = thrown(CuisineNotFoundException)
        ex.message == 'Cuisine not found with id: 19'
        0 * _
    }

    def "removeRecipeCuisine returns early when association does not exist"() {
        when:
        service.removeRecipeCuisine(20L, 21)

        then:
        1 * recipeCuisineRepository.existsByRecipeIdAndCuisineId(20L, 21) >> false
        0 * _
    }

    def "removeRecipeCuisine deletes association when present"() {
        when:
        service.removeRecipeCuisine(22L, 23)

        then:
        1 * recipeCuisineRepository.existsByRecipeIdAndCuisineId(22L, 23) >> true
        1 * recipeCuisineRepository.deleteByRecipeIdAndCuisineId(22L, 23)
        0 * _
    }

    def "removeAllRecipeCuisines throws when recipe missing"() {
        when:
        service.removeAllRecipeCuisines(24L)

        then:
        1 * recipeRepository.existsById(24L) >> false
        def ex = thrown(CuisineNotFoundException)
        ex.message == 'Recipe not found with id: 24'
        0 * _
    }

    def "removeAllRecipeCuisines deletes all associations when recipe exists"() {
        when:
        service.removeAllRecipeCuisines(25L)

        then:
        1 * recipeRepository.existsById(25L) >> true
        1 * recipeCuisineRepository.deleteByRecipeId(25L)
        0 * _
    }

    def "isRecipeAssociatedWithCuisine delegates to repository"() {
        when:
        def result = service.isRecipeAssociatedWithCuisine(26L, 27)

        then:
        1 * recipeCuisineRepository.existsByRecipeIdAndCuisineId(26L, 27) >> true
        result
        0 * _
    }

    def "getRecipeCuisineList returns cuisine names"() {
        given:
        def recipeId = 28L
        def italian = new Cuisine()
        italian.name = 'Italian'
        def mexican = new Cuisine()
        mexican.name = 'Mexican'
        def association1 = new RecipeCuisine()
        association1.cuisine = italian
        def association2 = new RecipeCuisine()
        association2.cuisine = mexican

        when:
        def result = service.getRecipeCuisineList(recipeId)

        then:
        1 * recipeRepository.existsById(recipeId) >> true
        1 * recipeCuisineRepository.findByRecipeId(recipeId) >> [association1, association2]
        result == ['Italian', 'Mexican']
        0 * _
    }

    def "getRecipeCuisineList throws when no cuisines found"() {
        when:
        service.getRecipeCuisineList(29L)

        then:
        1 * recipeRepository.existsById(29L) >> true
        1 * recipeCuisineRepository.findByRecipeId(29L) >> []
        def ex = thrown(CuisineNotFoundException)
        ex.message == 'No cuisines found for recipe with id: 29'
        0 * _
    }

    def "updateRecipeCuisines clears existing links before adding new ones"() {
        given:
        def recipeId = 30L
        def cuisineIds = [31, 32] as Set
        def recipeRef = new Recipe()
        recipeRef.id = recipeId
        def cuisine31 = new Cuisine()
        cuisine31.id = 31
        cuisine31.name = 'Cuisine 31'
        def cuisine32 = new Cuisine()
        cuisine32.id = 32
        cuisine32.name = 'Cuisine 32'
        def cuisinesById = [(31): cuisine31, (32): cuisine32]
        def savedCuisineIds = [] as List<Integer>

        when:
        service.updateRecipeCuisines(recipeId, cuisineIds)

        then:
        1 * recipeRepository.existsById(recipeId) >> true
        1 * recipeCuisineRepository.deleteByRecipeId(recipeId)
        1 * recipeRepository.existsById(recipeId) >> true
        2 * recipeCuisineRepository.existsByRecipeIdAndCuisineId(recipeId, _ as Integer) >> false
        2 * cuisineRepository.findById(_ as Integer) >> { Integer id -> Optional.of(cuisinesById[id]) }
        2 * recipeRepository.getReferenceById(recipeId) >> recipeRef
        2 * recipeCuisineRepository.save({ RecipeCuisine rc ->
            savedCuisineIds << rc.id.cuisineId
            assert rc.recipe.is(recipeRef)
            assert rc.cuisine.is(cuisinesById[rc.id.cuisineId])
            true
        }) >> { RecipeCuisine rc -> rc }
        savedCuisineIds as Set == cuisineIds
        0 * _
    }

    def "updateRecipeCuisines throws when recipe missing"() {
        when:
        service.updateRecipeCuisines(33L, [34] as Set)

        then:
        1 * recipeRepository.existsById(33L) >> false
        def ex = thrown(CuisineNotFoundException)
        ex.message == 'Recipe not found with id: 33'
        0 * _
    }

    def "getCuisineNamesByRecipeId returns mapped names"() {
        given:
        def recipeId = 35L
        def cuisine = new Cuisine()
        cuisine.name = 'Fusion'
        def association = new RecipeCuisine()
        association.cuisine = cuisine

        when:
        def result = service.getCuisineNamesByRecipeId(recipeId)

        then:
        1 * recipeCuisineRepository.findByRecipeId(recipeId) >> [association]
        result == ['Fusion']
        0 * _
    }
}

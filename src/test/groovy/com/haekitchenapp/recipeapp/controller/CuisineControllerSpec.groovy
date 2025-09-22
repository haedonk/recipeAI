package com.haekitchenapp.recipeapp.controller

import com.haekitchenapp.recipeapp.entity.Cuisine
import com.haekitchenapp.recipeapp.exception.CuisineNotFoundException
import com.haekitchenapp.recipeapp.service.CuisineService
import com.haekitchenapp.recipeapp.service.RecipeCuisineService
import com.haekitchenapp.recipeapp.service.RecipeMealService
import spock.lang.Specification

import java.util.Optional

class CuisineControllerSpec extends Specification {

    CuisineController cuisineController
    CuisineService cuisineService
    RecipeCuisineService recipeCuisineService
    RecipeMealService recipeMealService

    def setup() {
        cuisineService = Mock(CuisineService)
        recipeCuisineService = Mock(RecipeCuisineService)
        recipeMealService = Mock(RecipeMealService)
        cuisineController = new CuisineController(cuisineService, recipeCuisineService, recipeMealService)
    }

    def "returns all cuisines"() {
        given:
        def cuisines = [new Cuisine(id: 1, name: 'Italian'), new Cuisine(id: 2, name: 'Thai')]

        when:
        def response = cuisineController.getAllCuisines()

        then:
        1 * cuisineService.findAll() >> cuisines
        response.statusCode.value() == 200
        response.body == cuisines
    }

    def "returns cuisine by id when present"() {
        given:
        def cuisine = new Cuisine(id: 3, name: 'Mexican')

        when:
        def response = cuisineController.getCuisineById(3)

        then:
        1 * cuisineService.findById(3) >> Optional.of(cuisine)
        response.statusCode.value() == 200
        response.body == cuisine
    }

    def "throws exception when cuisine is missing"() {
        given:
        cuisineService.findById(55) >> Optional.empty()

        when:
        cuisineController.getCuisineById(55)

        then:
        thrown(CuisineNotFoundException)
    }

    def "creates cuisine and returns 201"() {
        given:
        def request = new Cuisine(name: 'Indian')
        def saved = new Cuisine(id: 10, name: 'Indian')

        when:
        def response = cuisineController.createCuisine(request)

        then:
        1 * cuisineService.createCuisine(request) >> saved
        response.statusCode.value() == 201
        response.body == saved
    }

    def "updates cuisine and returns updated entity"() {
        given:
        def request = new Cuisine(name: 'Updated')
        def updated = new Cuisine(id: 8, name: 'Updated')

        when:
        def response = cuisineController.updateCuisine(8, request)

        then:
        1 * cuisineService.updateCuisine(8, request) >> updated
        response.statusCode.value() == 200
        response.body == updated
    }

    def "deletes cuisine and returns no content"() {
        when:
        def response = cuisineController.deleteCuisine(4)

        then:
        1 * cuisineService.deleteCuisine(4)
        response.statusCode.value() == 204
        response.body == null
    }

    def "searches cuisines by name"() {
        given:
        def cuisines = [new Cuisine(id: 5, name: 'Italian')]

        when:
        def response = cuisineController.searchCuisinesByName('It')

        then:
        1 * cuisineService.findByNameContaining('It') >> cuisines
        response.statusCode.value() == 200
        response.body == cuisines
    }

    def "gets cuisines for recipe"() {
        given:
        def cuisineNames = ['Italian', 'Mexican']

        when:
        def response = cuisineController.getCuisinesForRecipe(11L)

        then:
        1 * recipeCuisineService.getRecipeCuisineList(11L) >> cuisineNames
        response.statusCode.value() == 200
        response.body == cuisineNames
    }

    def "associates recipe with cuisine"() {
        when:
        def response = cuisineController.associateRecipeWithCuisine(12L, 7)

        then:
        1 * recipeCuisineService.associateRecipeWithCuisine(12L, 7)
        response.statusCode.value() == 200
        response.body == 'Recipe successfully associated with cuisine'
    }

    def "associates recipe with cuisines"() {
        given:
        def cuisineIds = [1, 2] as Set

        when:
        def response = cuisineController.associateRecipeWithCuisines(13L, cuisineIds)

        then:
        1 * recipeCuisineService.associateRecipeWithCuisines(13L, cuisineIds)
        response.statusCode.value() == 200
        response.body == 'Recipe successfully associated with cuisines'
    }

    def "updates recipe cuisines"() {
        given:
        def cuisineIds = [3, 4] as Set

        when:
        def response = cuisineController.updateRecipeCuisines(14L, cuisineIds)

        then:
        1 * recipeCuisineService.updateRecipeCuisines(14L, cuisineIds)
        response.statusCode.value() == 200
        response.body == 'Recipe cuisines updated successfully'
    }

    def "removes recipe cuisine association"() {
        when:
        def response = cuisineController.removeRecipeCuisine(15L, 9)

        then:
        1 * recipeCuisineService.removeRecipeCuisine(15L, 9)
        response.statusCode.value() == 200
        response.body == 'Association removed successfully'
    }

    def "removes all cuisine associations for recipe"() {
        when:
        def response = cuisineController.removeAllRecipeCuisines(16L)

        then:
        1 * recipeCuisineService.removeAllRecipeCuisines(16L)
        response.statusCode.value() == 200
        response.body == 'All cuisine associations removed successfully'
    }

    def "checks if recipe is associated with cuisine"() {
        when:
        def response = cuisineController.isRecipeAssociatedWithCuisine(17L, 6)

        then:
        1 * recipeCuisineService.isRecipeAssociatedWithCuisine(17L, 6) >> true
        response.statusCode.value() == 200
        response.body == true
    }

    def "associates recipe with meal types"() {
        given:
        def mealTypeIds = [ (short) 1, (short) 2 ] as Set

        when:
        def response = cuisineController.associateRecipeWithMealTypes(18L, mealTypeIds)

        then:
        1 * recipeMealService.associateRecipeWithmealTypes(18L, mealTypeIds)
        response.statusCode.value() == 200
        response.body == 'Recipe successfully associated with meal types'
    }
}

package com.haekitchenapp.recipeapp.service

import com.haekitchenapp.recipeapp.entity.Unit
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeIngredientRequest
import com.haekitchenapp.recipeapp.model.response.ApiResponse
import com.haekitchenapp.recipeapp.repository.UnitRepository
import org.springframework.http.ResponseEntity
import spock.lang.Specification

import java.util.Optional

class UnitServiceSpec extends Specification {

    UnitService service
    UnitRepository unitRepository

    def setup() {
        service = new UnitService()
        unitRepository = Mock(UnitRepository)
        service.@unitRepository = unitRepository
        service.@unitCache = null
    }

    def "getAllUnits returns repository results"() {
        given:
        service.@unitCache = null
        List<Unit> units = [
                new Unit(id: 1L, name: 'cup'),
                new Unit(id: 2L, name: 'tablespoon')
        ]

        when:
        ResponseEntity<ApiResponse<List<Unit>>> response = service.getAllUnits()

        then:
        1 * unitRepository.findAll() >> units
        response.body.success
        response.body.message == 'Units retrieved successfully'
        response.body.data == units
    }

    def "getAllUnitsMap caches repository results after first call"() {
        given:
        service.@unitCache = null
        List<Unit> units = [
                new Unit(id: 3L, name: 'gram'),
                new Unit(id: 4L, name: 'ounce')
        ]

        when:
        Map<Long, String> firstCall = service.getAllUnitsMap()
        Map<Long, String> secondCall = service.getAllUnitsMap()

        then:
        1 * unitRepository.findAll() >> units
        firstCall == [3L: 'gram', 4L: 'ounce']
        secondCall.is(firstCall)
        service.@unitCache.is(firstCall)
        0 * unitRepository._
    }

    def "getUnitNameById falls back to Unknown Unit when cache lacks key"() {
        given:
        service.@unitCache = [5L: 'liter']
        List<Unit> units = [new Unit(id: 5L, name: 'liter')]

        when:
        String result = service.getUnitNameById(42L)

        then:
        1 * unitRepository.findAll() >> units
        result == 'Unknown Unit'
        service.@unitCache == [5L: 'liter']
        0 * unitRepository._
    }

    def "existsById handles null and uses cached map for lookups"() {
        given:
        service.@unitCache = null
        List<Unit> units = [new Unit(id: 7L, name: 'dash')]

        when:
        boolean nullResult = service.existsById(null)
        boolean firstLookup = service.existsById(7L)
        boolean secondLookup = service.existsById(7L)

        then:
        nullResult == false
        firstLookup
        secondLookup
        1 * unitRepository.findAll() >> units
        service.@unitCache == [7L: 'dash']
        0 * unitRepository._
    }

    def "persistAiGeneratedUnits saves new lowercased units and refreshes cache"() {
        given:
        service.@unitCache = [99L: 'stale']
        Unit existingUnit = new Unit(id: 10L, name: 'teaspoon', aiGenerated: false)
        Set<RecipeIngredientRequest> ingredients = [
                ingredient('Cup', 'Flour'),
                ingredient('cup', 'Sugar'),
                ingredient('TEASPOON', 'Pepper'),
                ingredient('Pinch', 'Salt'),
                ingredient('  ', 'Blank'),
                ingredient(null, 'None')
        ] as Set

        and:
        List<Unit> refreshedUnits = [
                existingUnit,
                new Unit(id: 20L, name: 'cup', aiGenerated: true),
                new Unit(id: 21L, name: 'pinch', aiGenerated: true)
        ]

        when:
        service.persistAiGeneratedUnits(ingredients)

        then:
        3 * unitRepository.findByName({ it == it.toLowerCase() }) >> { String name ->
            name == 'teaspoon' ? Optional.of(existingUnit) : Optional.empty()
        }
        1 * unitRepository.saveAll({ List<Unit> saved ->
            assert saved*.name as Set == ['cup', 'pinch'] as Set
            assert saved.every { it.aiGenerated }
            true
        }) >> { List<Unit> saved -> saved }
        1 * unitRepository.findAll() >> refreshedUnits
        service.@unitCache == [10L: 'teaspoon', 20L: 'cup', 21L: 'pinch']
        0 * unitRepository._
    }

    private static RecipeIngredientRequest ingredient(String unitName, String ingredientName) {
        RecipeIngredientRequest request = new RecipeIngredientRequest()
        request.setName(ingredientName)
        request.setQuantity('1')
        request.setUnitName(unitName)
        return request
    }
}

package com.haekitchenapp.recipeapp.service

import com.haekitchenapp.recipeapp.entity.Unit
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeIngredientRequest
import com.haekitchenapp.recipeapp.model.response.ApiResponse
import com.haekitchenapp.recipeapp.repository.UnitRepository
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class UnitServiceSpec extends Specification {

    UnitService service
    UnitRepository unitRepository

    def setup() {
        unitRepository = Mock(UnitRepository)
        service = new UnitService(unitRepository)
    }

    def "getAllUnits returns repository results"() {
        given:
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
        secondCall == [3L: 'gram', 4L: 'ounce']
    }

    def "getUnitNameById returns unit name when found in cache"() {
        given:
        List<Unit> units = [new Unit(id: 5L, name: 'liter')]
        unitRepository.findAll() >> units

        when:
        String result = service.getUnitNameById(5L)

        then:
        result == 'liter'
    }

    def "getUnitNameById returns 'Unknown Unit' when not found"() {
        given:
        unitRepository.findAll() >> []

        when:
        String result = service.getUnitNameById(999L)

        then:
        result == 'Unknown Unit'
    }

    def "getUnitByName returns unit when found"() {
        given:
        Unit unit = new Unit(id: 6L, name: 'pound')

        when:
        Unit result = service.getUnitByName('pound')

        then:
        1 * unitRepository.findByName('pound') >> Optional.of(unit)
        result == unit
    }

    def "getUnitByName returns null when not found"() {
        when:
        Unit result = service.getUnitByName('nonexistent')

        then:
        1 * unitRepository.findByName('nonexistent') >> Optional.empty()
        result == null
    }

    def "existsById returns true when unit exists in cache"() {
        given:
        List<Unit> units = [new Unit(id: 7L, name: 'kilogram')]
        unitRepository.findAll() >> units

        when:
        boolean result = service.existsById(7L)

        then:
        result
    }

    def "existsById returns false when unit does not exist"() {
        given:
        unitRepository.findAll() >> []

        when:
        boolean result = service.existsById(999L)

        then:
        !result
    }

    def "existsById returns false for null input"() {
        when:
        boolean result = service.existsById(null)

        then:
        !result
        0 * unitRepository._
    }

    def "persistAiGeneratedUnits saves new units"() {
        given:
        Set<RecipeIngredientRequest> ingredients = [
                new RecipeIngredientRequest(unitName: 'newUnit1'),
                new RecipeIngredientRequest(unitName: 'newUnit2'),
                new RecipeIngredientRequest(unitName: 'existingUnit')
        ] as Set

        Unit existingUnit = new Unit(id: 1L, name: 'existingunit')

        when:
        service.persistAiGeneratedUnits(ingredients)

        then:
        1 * unitRepository.findByName('newunit1') >> Optional.empty()
        1 * unitRepository.findByName('newunit2') >> Optional.empty()
        1 * unitRepository.findByName('existingunit') >> Optional.of(existingUnit)
        1 * unitRepository.saveAll({ List<Unit> units ->
            units.size() == 2 &&
            units.any { it.name == 'newunit1' && it.aiGenerated } &&
            units.any { it.name == 'newunit2' && it.aiGenerated }
        })
        1 * unitRepository.findAll() >> []
    }

    def "persistAiGeneratedUnits does nothing when no new units"() {
        given:
        Set<RecipeIngredientRequest> ingredients = [
                new RecipeIngredientRequest(unitName: 'existingUnit')
        ] as Set

        Unit existingUnit = new Unit(id: 1L, name: 'existingunit')

        when:
        service.persistAiGeneratedUnits(ingredients)

        then:
        1 * unitRepository.findByName('existingunit') >> Optional.of(existingUnit)
        0 * unitRepository.saveAll(_)
    }

    def "persistAiGeneratedUnits handles empty and null unit names"() {
        given:
        Set<RecipeIngredientRequest> ingredients = [
                new RecipeIngredientRequest(unitName: null),
                new RecipeIngredientRequest(unitName: ''),
                new RecipeIngredientRequest(unitName: '   '),
                new RecipeIngredientRequest(unitName: 'validUnit')
        ] as Set

        when:
        service.persistAiGeneratedUnits(ingredients)

        then:
        1 * unitRepository.findByName('validunit') >> Optional.empty()
        1 * unitRepository.saveAll({ List<Unit> units ->
            units.size() == 1 && units[0].name == 'validunit'
        })
        1 * unitRepository.findAll() >> []
    }
}

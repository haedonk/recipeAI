package com.haekitchenapp.recipeapp.service

import com.haekitchenapp.recipeapp.entity.Ingredient
import com.haekitchenapp.recipeapp.exception.IngredientException
import com.haekitchenapp.recipeapp.repository.IngredientRepository
import spock.lang.Specification
import spock.lang.Unroll

class IngredientServiceSpec extends Specification {

    IngredientRepository ingredientRepository
    IngredientService service

    def setup() {
        ingredientRepository = Mock()
        service = new IngredientService(ingredientRepository)
    }

    def "initial cache population occurs once and repeated lookups use cache"() {
        given:
        def salt = ingredient(1L, "salt")

        when:
        def first = service.getIngredientNameById(1L)

        then:
        first == "salt"
        service.@ingredientCache[1L].is(salt)
        service.@ingredientNameCache["salt"].is(salt)
        service.@countOnLoad == 1
        service.@lastIdOnLoad == 1L
        1 * ingredientRepository.count() >> 1
        1 * ingredientRepository.findAllGreaterThanId(0L) >> [salt]
        0 * ingredientRepository._

        when:
        def second = service.getIngredientNameById(1L)

        then:
        second == "salt"
        0 * ingredientRepository._
    }

    def "getIngredientElseInsert returns cached ingredient without repository access"() {
        given:
        def garlic = ingredient(10L, "garlic")
        service.@ingredientCache.put(10L, garlic)
        service.@ingredientNameCache.put("garlic", garlic)

        when:
        def result = service.getIngredientElseInsert(" Garlic ")

        then:
        result.is(garlic)
        0 * ingredientRepository._
    }

    @Unroll
    def "getIngredientElseInsert rejects invalid name '#invalidName'"(String invalidName) {
        when:
        service.getIngredientElseInsert(invalidName)

        then:
        thrown IngredientException
        0 * ingredientRepository._

        where:
        invalidName << [null, "", "   "]
    }

    def "getIngredientElseInsert saves new ingredient and updates caches"() {
        given:
        def saved = ingredient(7L, "new spice")

        when:
        def result = service.getIngredientElseInsert("  New Spice  ")

        then:
        result.is(saved)
        service.@ingredientCache[7L].is(saved)
        service.@ingredientNameCache["new spice"].is(saved)
        service.@lastIdOnLoad == 7L
        service.@countOnLoad == 1
        1 * ingredientRepository.count() >> 0
        1 * ingredientRepository.save({ Ingredient toSave ->
            assert toSave.id == null
            assert toSave.name == "new spice"
            true
        }) >> saved
        0 * ingredientRepository._
    }

    def "getIngredientNameById refreshes cache on miss and handles null id and missing results"() {
        given:
        def pepper = ingredient(2L, "pepper")

        when:
        def result = service.getIngredientNameById(2L)

        then:
        result == "pepper"
        service.@ingredientCache[2L].is(pepper)
        service.@ingredientNameCache["pepper"].is(pepper)
        1 * ingredientRepository.count() >> 1
        1 * ingredientRepository.findAllGreaterThanId(0L) >> [pepper]
        0 * ingredientRepository._

        when:
        def missing = service.getIngredientNameById(99L)

        then:
        missing == null
        1 * ingredientRepository.count() >> 1
        0 * ingredientRepository._

        when:
        service.getIngredientNameById(null)

        then:
        thrown IngredientException
        0 * ingredientRepository._
    }

    def "getIngredientNameByName uses cache validates input"() {
        given:
        def basil = ingredient(3L, "basil")
        service.@ingredientNameCache.put("basil", basil)

        expect:
        service.getIngredientNameByName("Basil") == "basil"
        service.getIngredientNameByName("thyme") == null

        when:
        service.getIngredientNameByName(null)

        then:
        thrown IngredientException
        0 * ingredientRepository._
    }

    private static Ingredient ingredient(long id, String name) {
        def ingredient = new Ingredient()
        ingredient.setId(id)
        ingredient.setName(name)
        return ingredient
    }
}

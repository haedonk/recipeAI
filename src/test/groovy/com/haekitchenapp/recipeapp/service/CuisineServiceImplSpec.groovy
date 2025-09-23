package com.haekitchenapp.recipeapp.service

import com.haekitchenapp.recipeapp.entity.Cuisine
import com.haekitchenapp.recipeapp.exception.CuisineNotFoundException
import com.haekitchenapp.recipeapp.repository.CuisineRepository
import com.haekitchenapp.recipeapp.service.impl.CuisineServiceImpl
import spock.lang.Specification

import java.util.Optional

class CuisineServiceImplSpec extends Specification {

    CuisineRepository cuisineRepository
    CuisineServiceImpl cuisineService

    def setup() {
        cuisineRepository = Mock(CuisineRepository)
        cuisineService = new CuisineServiceImpl(cuisineRepository)
    }

    def "findById returns repository result"() {
        given:
        def expected = Optional.of(new Cuisine())

        when:
        def result = cuisineService.findById(123)

        then:
        1 * cuisineRepository.findById(123) >> expected
        0 * _
        result == expected
    }

    def "findByName returns repository result"() {
        given:
        def expected = Optional.of(new Cuisine())

        when:
        def result = cuisineService.findByName("Italian")

        then:
        1 * cuisineRepository.findByName("Italian") >> expected
        0 * _
        result == expected
    }

    def "findAll returns repository result"() {
        given:
        def cuisines = [new Cuisine(), new Cuisine()]

        when:
        def result = cuisineService.findAll()

        then:
        1 * cuisineRepository.findAll() >> cuisines
        0 * _
        result == cuisines
    }

    def "findByNameContaining returns repository result"() {
        given:
        def cuisines = [new Cuisine()]

        when:
        def result = cuisineService.findByNameContaining("an")

        then:
        1 * cuisineRepository.findByNameContainingIgnoreCase("an") >> cuisines
        0 * _
        result == cuisines
    }

    def "findByRecipeId returns repository result"() {
        given:
        def cuisines = [new Cuisine()]

        when:
        def result = cuisineService.findByRecipeId(5L)

        then:
        1 * cuisineRepository.findByRecipeId(5L) >> cuisines
        0 * _
        result == cuisines
    }

    def "createCuisine saves entity"() {
        given:
        def cuisine = new Cuisine()
        cuisine.setName("Thai")
        def saved = new Cuisine()
        saved.setId(1)
        saved.setName("Thai")

        when:
        def result = cuisineService.createCuisine(cuisine)

        then:
        1 * cuisineRepository.save(cuisine) >> saved
        0 * _
        result == saved
    }

    def "updateCuisine saves existing cuisine"() {
        given:
        def update = new Cuisine()
        update.setName("New Name")
        def existing = new Cuisine()
        existing.setId(10)
        existing.setName("Old Name")

        when:
        def result = cuisineService.updateCuisine(10, update)

        then:
        1 * cuisineRepository.findById(10) >> Optional.of(existing)
        1 * cuisineRepository.save({ Cuisine saved ->
            assert saved.is(existing)
            assert saved.name == "New Name"
            true
        }) >> { Cuisine saved -> saved }
        0 * _
        result.is(existing)
        result.name == "New Name"
    }

    def "updateCuisine throws when not found"() {
        when:
        cuisineService.updateCuisine(99, new Cuisine())

        then:
        1 * cuisineRepository.findById(99) >> Optional.empty()
        0 * cuisineRepository.save(_)
        def ex = thrown(CuisineNotFoundException)
        ex.message == "Cuisine not found with id: 99"
    }

    def "deleteCuisine throws when cuisine missing"() {
        when:
        cuisineService.deleteCuisine(7)

        then:
        1 * cuisineRepository.existsById(7) >> false
        0 * cuisineRepository.deleteById(_)
        thrown(CuisineNotFoundException)
    }

    def "deleteCuisine removes cuisine when present"() {
        when:
        cuisineService.deleteCuisine(8)

        then:
        1 * cuisineRepository.existsById(8) >> true
        1 * cuisineRepository.deleteById(8)
        0 * _
    }

    def "existsByName returns repository response"() {
        when:
        def result = cuisineService.existsByName("Mexican")

        then:
        1 * cuisineRepository.existsByName("Mexican") >> true
        0 * _
        result
    }
}

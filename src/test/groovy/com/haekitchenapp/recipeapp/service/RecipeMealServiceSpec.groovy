package com.haekitchenapp.recipeapp.service

import com.haekitchenapp.recipeapp.entity.MealType
import com.haekitchenapp.recipeapp.entity.Recipe
import com.haekitchenapp.recipeapp.entity.RecipeMeal
import com.haekitchenapp.recipeapp.entity.composite.RecipeMealId
import com.haekitchenapp.recipeapp.repository.MealTypeRepository
import com.haekitchenapp.recipeapp.repository.RecipeMealRepository
import com.haekitchenapp.recipeapp.repository.RecipeRepository
import spock.lang.Specification

class RecipeMealServiceSpec extends Specification {

    RecipeMealRepository recipeMealRepository
    RecipeRepository recipeRepository
    MealTypeRepository mealTypeRepository
    RecipeMealService recipeMealService

    def setup() {
        recipeMealRepository = Mock()
        recipeRepository = Mock()
        mealTypeRepository = Mock()
        recipeMealService = new RecipeMealService(recipeMealRepository, recipeRepository, mealTypeRepository)
    }

    def "insert saves new association when mapping absent"() {
        given:
        def recipe = new Recipe()
        recipe.id = 10L
        def mealType = new MealType()
        mealType.id = (short) 3

        when:
        RecipeMeal result = recipeMealService.insert(recipe, mealType)

        then:
        1 * recipeMealRepository.existsById({ RecipeMealId id ->
            id.recipeId == 10L && id.mealTypeId == (short) 3
        }) >> false
        1 * recipeMealRepository.save({ RecipeMeal saved ->
            assert saved.recipe.is(recipe)
            assert saved.mealType.is(mealType)
            assert saved.id.recipeId == 10L
            assert saved.id.mealTypeId == (short) 3
            true
        }) >> { RecipeMeal saved -> saved }

        result.recipe.is(recipe)
        result.mealType.is(mealType)
        result.id.recipeId == 10L
        result.id.mealTypeId == (short) 3
    }

    def "insert returns null when association already exists"() {
        given:
        def recipe = new Recipe()
        recipe.id = 11L
        def mealType = new MealType()
        mealType.id = (short) 4

        when:
        def result = recipeMealService.insert(recipe, mealType)

        then:
        1 * recipeMealRepository.existsById({ RecipeMealId id ->
            id.recipeId == 11L && id.mealTypeId == (short) 4
        }) >> true
        0 * recipeMealRepository.save(_ as RecipeMeal)
        result == null
    }

    def "associateRecipeWithmealTypes saves new associations for each meal type"() {
        given:
        Long recipeId = 42L
        def mealIds = [1 as Short, 2 as Short, 3 as Short] as Set
        def recipe = new Recipe()
        recipe.id = recipeId

        def mealTypeLookup = mealIds.collectEntries { Short id ->
            def mealType = new MealType()
            mealType.id = id
            mealType.name = "Meal ${id}"
            [(id): mealType]
        }

        def checkedAssociations = []
        def savedMealIds = []

        recipeRepository.existsById(recipeId) >> true
        mealTypeRepository.existsById(_ as Short) >> { Short id -> mealIds.contains(id) }
        recipeRepository.getReferenceById(recipeId) >> recipe
        mealTypeRepository.getReferenceById(_ as Short) >> { Short id -> mealTypeLookup[id] }
        recipeMealRepository.existsById(_ as RecipeMealId) >> { RecipeMealId id ->
            checkedAssociations << id.mealTypeId
            id.mealTypeId == (short) 2
        }
        recipeMealRepository.save(_ as RecipeMeal) >> { RecipeMeal saved ->
            savedMealIds << saved.id.mealTypeId
            assert saved.id.recipeId == recipeId
            assert saved.recipe.is(recipe)
            assert saved.mealType.is(mealTypeLookup[saved.id.mealTypeId])
            saved
        }

        when:
        recipeMealService.associateRecipeWithmealTypes(recipeId, mealIds)

        then:
        checkedAssociations as Set == mealIds
        checkedAssociations.size() == mealIds.size()
        savedMealIds.size() == 2
        savedMealIds as Set == [1 as Short, 3 as Short] as Set
    }

    def "associateRecipeWithmealTypes throws when recipe missing"() {
        given:
        Long recipeId = 99L
        def mealIds = [5 as Short] as Set

        recipeRepository.existsById(recipeId) >> false
        mealTypeRepository.existsById(_ as Short) >> true

        when:
        recipeMealService.associateRecipeWithmealTypes(recipeId, mealIds)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Recipe or MealType does not exist"
        0 * recipeRepository.getReferenceById(_ as Long)
        0 * mealTypeRepository.getReferenceById(_ as Short)
        0 * recipeMealRepository.existsById(_ as RecipeMealId)
        0 * recipeMealRepository.save(_ as RecipeMeal)
    }

    def "associateRecipeWithmealTypes throws when meal type missing"() {
        given:
        Long recipeId = 100L
        def mealIds = [7 as Short, 9 as Short] as Set

        recipeRepository.existsById(recipeId) >> true
        mealTypeRepository.existsById(_ as Short) >> { Short id -> id != (short) 9 }

        when:
        recipeMealService.associateRecipeWithmealTypes(recipeId, mealIds)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Recipe or MealType does not exist"
        0 * recipeRepository.getReferenceById(_ as Long)
        0 * mealTypeRepository.getReferenceById(_ as Short)
        0 * recipeMealRepository.existsById(_ as RecipeMealId)
        0 * recipeMealRepository.save(_ as RecipeMeal)
    }
}

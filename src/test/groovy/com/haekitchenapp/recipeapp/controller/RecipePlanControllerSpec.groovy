package com.haekitchenapp.recipeapp.controller

import com.haekitchenapp.recipeapp.model.request.recipe.BulkRecipePlanRequest
import com.haekitchenapp.recipeapp.model.response.ApiResponse
import com.haekitchenapp.recipeapp.model.response.recipe.RecipePlanResponse
import com.haekitchenapp.recipeapp.service.JwtTokenService
import com.haekitchenapp.recipeapp.service.RecipePlanService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import spock.lang.Specification

import java.time.LocalDate

class RecipePlanControllerSpec extends Specification {

    RecipePlanController recipePlanController
    RecipePlanService recipePlanService
    JwtTokenService jwtTokenService

    def setup() {
        recipePlanService = Mock(RecipePlanService)
        jwtTokenService = Mock(JwtTokenService)
        recipePlanController = new RecipePlanController(recipePlanService, jwtTokenService)
    }

    def "returns recipe plans for the authenticated user"() {
        given:
        def request = Mock(HttpServletRequest)
        def startDate = "2024-01-01"
        def endDate = "2024-01-07"
        def plans = [new RecipePlanResponse(1L, "alice", LocalDate.parse("2024-01-01"), "Breakfast", 10L, "Omelette", null, null, true)]

        when:
        def response = recipePlanController.getUserRecipePlans(startDate, endDate, request)

        then:
        1 * jwtTokenService.getUserIdFromRequest(request) >> 42L
        1 * recipePlanService.getPlansInDateRange(42L, startDate, endDate) >>
                ResponseEntity.ok(ApiResponse.success("Plans retrieved", plans))

        and:
        response.statusCode.value() == 200
        response.body.success
        response.body.data == plans
    }

    def "creates bulk recipe plans for the authenticated user"() {
        given:
        def request = Mock(HttpServletRequest)
        def bulkRequests = [
                new BulkRecipePlanRequest(
                        planDate: LocalDate.parse("2024-01-03"),
                        mealTypeId: (short) 2,
                        recipeId: 15L,
                        customTitle: "Dinner",
                        notes: "Family meal"
                )
        ]
        def createdPlans = [new RecipePlanResponse(5L, "alice", LocalDate.parse("2024-01-03"), "Dinner", 15L, "Pasta", "Dinner", "Family meal", true)]

        when:
        def response = recipePlanController.createBulkRecipePlans(bulkRequests, request)

        then:
        1 * jwtTokenService.getUserIdFromRequest(request) >> 7L
        1 * recipePlanService.createBulkRecipePlans(7L, bulkRequests) >>
                ResponseEntity.ok(ApiResponse.success("Created", createdPlans))

        and:
        response.statusCode.value() == 200
        response.body.data == createdPlans
    }

    def "deletes bulk recipe plans for the authenticated user"() {
        given:
        def request = Mock(HttpServletRequest)
        def planIds = [3L, 4L]

        when:
        def response = recipePlanController.deleteBulkRecipePlans(planIds, request)

        then:
        1 * jwtTokenService.getUserIdFromRequest(request) >> 99L
        1 * recipePlanService.deleteBulkRecipePlans(99L, planIds) >>
                ResponseEntity.ok(ApiResponse.success("Deleted"))

        and:
        response.statusCode.value() == 200
        response.body.message == "Deleted"
    }

    def "toggles saved status for recipe plans in a date range"() {
        given:
        def request = Mock(HttpServletRequest)
        def startDate = "2024-01-01"
        def endDate = "2024-01-31"
        def saved = true

        when:
        def response = recipePlanController.toggleSavedStatusBulk(startDate, endDate, saved, request)

        then:
        1 * jwtTokenService.getUserIdFromRequest(request) >> 55L
        1 * recipePlanService.toggleSavedStatusBulk(55L, startDate, endDate, saved) >>
                ResponseEntity.ok(ApiResponse.success("Toggled", "Saved"))

        and:
        response.statusCode.value() == 200
        response.body.data == "Saved"
    }

    def "propagates service errors when listing plans fails"() {
        given:
        def request = Mock(HttpServletRequest)
        def startDate = "2024-02-01"
        def endDate = "2024-02-07"

        when:
        recipePlanController.getUserRecipePlans(startDate, endDate, request)

        then:
        1 * jwtTokenService.getUserIdFromRequest(request) >> 12L
        1 * recipePlanService.getPlansInDateRange(12L, startDate, endDate) >> { throw new IllegalStateException("Service unavailable") }
        thrown(IllegalStateException)
    }
}

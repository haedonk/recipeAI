package com.haekitchenapp.recipeapp.controller

import com.haekitchenapp.recipeapp.exception.RecipeNotFoundException
import com.haekitchenapp.recipeapp.model.request.recipe.RecipeRequest
import com.haekitchenapp.recipeapp.model.response.ApiResponse
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeResponse
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeTitleDto
import com.haekitchenapp.recipeapp.security.JwtUtils
import com.haekitchenapp.recipeapp.service.RecipeAIService
import com.haekitchenapp.recipeapp.service.RecipeService
import com.haekitchenapp.recipeapp.service.UnitService
import com.haekitchenapp.recipeapp.service.UserDetailsServiceImpl
import com.haekitchenapp.recipeapp.support.Fixtures
import com.haekitchenapp.recipeapp.support.JsonSupport
import org.hamcrest.Matchers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(controllers = RecipeController)
@Import([Advice, com.haekitchenapp.recipeapp.config.security.WebSecurityConfig])
@TestPropertySource(properties = [
        'spring.security.jwt.secret=ZmFrZXNlY3JldGZha2VzZWNyZXQ=',
        'spring.security.jwt.expiration=3600'
])
class RecipeControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    RecipeController recipeController

    @MockBean
    RecipeService recipeService

    @MockBean
    UnitService unitService

    @MockBean
    RecipeAIService recipeAIService

    @MockBean
    JwtUtils jwtUtils

    @MockBean
    UserDetailsServiceImpl userDetailsService

    def setup() {
        jwtUtils.validateJwtToken(_) >> false
    }

    @WithMockUser
    def "creates recipe when payload is valid"() {
        given:
        RecipeRequest request = Fixtures.recipeRequest()
        def recipe = Fixtures.recipe(id: 55L)
        recipeService.create(_ as RecipeRequest) >> ResponseEntity.ok(ApiResponse.success('Recipe created successfully', recipe))

        expect:
        mockMvc.perform(post('/api/recipes')
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSupport.write(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.success').value(true))
                .andExpect(jsonPath('$.data.id').value(55))
    }

    @WithMockUser
    def "returns 400 when payload is invalid"() {
        given:
        def body = [
                createdBy   : 1,
                instructions: 'Do something',
                ingredients : [[name: 'Salt', quantity: '1', unitId: 1]]
        ]

        expect:
        mockMvc.perform(post('/api/recipes')
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSupport.write(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath('$.message', Matchers.containsString('title')))
    }

    @WithMockUser
    def "gets recipe by id when it exists"() {
        given:
        def responseBody = new RecipeResponse(10L, 1L, 'Chili', 'Cook slowly', 'Spicy', null, [] as Set, 5, 30, 4)
        recipeService.findById(10L) >> ResponseEntity.ok(ApiResponse.success('Recipe retrieved successfully', responseBody))

        expect:
        mockMvc.perform(get('/api/recipes/10'))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data.title').value('Chili'))
    }

    @WithMockUser
    def "returns 404 when recipe is not found"() {
        given:
        recipeService.findById(100L) >> { throw new RecipeNotFoundException('Recipe not found with ID: 100') }

        expect:
        mockMvc.perform(get('/api/recipes/100'))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath('$.message').value('Recipe not found with ID: 100'))
    }

    @WithMockUser
    def "searches recipes by title"() {
        given:
        def dtos = [new RecipeTitleDto(1L, 'Toast', 'Instructions')]
        recipeService.searchByTitle('Toast') >> ResponseEntity.ok(ApiResponse.success('Recipes retrieved successfully', dtos))

        expect:
        mockMvc.perform(get('/api/recipes/search').param('title', 'Toast'))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.data[0].title').value('Toast'))
    }

    @WithMockUser
    def "returns 400 when search title is blank"() {
        given:
        recipeService.searchByTitle('') >> { throw new IllegalArgumentException('Title must not be null or empty') }

        expect:
        mockMvc.perform(get('/api/recipes/search').param('title', ''))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath('$.message').value('Invalid request: Title must not be null or empty'))
    }

    def "rejects unauthenticated access"() {
        expect:
        mockMvc.perform(get('/api/recipes/1'))
                .andExpect(status().isUnauthorized())
    }

    @WithMockUser
    def "deletes recipe by id"() {
        given:
        recipeService.deleteById(5L) >> ResponseEntity.ok(ApiResponse.success('Recipe deleted successfully'))

        expect:
        mockMvc.perform(delete('/api/recipes/5'))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.message').value('Recipe deleted successfully'))
    }
}

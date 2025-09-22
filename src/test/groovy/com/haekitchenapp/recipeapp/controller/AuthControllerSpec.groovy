package com.haekitchenapp.recipeapp.controller

import com.haekitchenapp.recipeapp.model.response.auth.JwtResponse
import com.haekitchenapp.recipeapp.model.response.auth.LoginRequest
import com.haekitchenapp.recipeapp.model.response.auth.RegisterRequest
import com.haekitchenapp.recipeapp.service.AuthService
import spock.lang.Specification

class AuthControllerSpec extends Specification {

    AuthController authController
    AuthService authService

    def setup() {
        authService = Mock(AuthService)
        authController = new AuthController(authService)
    }

    def "authenticates user successfully"() {
        given:
        def loginRequest = new LoginRequest("chef", "secret")
        def jwtResponse = new JwtResponse("token", 42L, "chef", "chef@example.com")
        authService.authenticateUser(_ as LoginRequest) >> jwtResponse

        when:
        def response = authController.authenticateUser(loginRequest)

        then:
        response.statusCode.value() == 200
        response.body.success
        response.body.data == null
        response.body.message == String.valueOf(jwtResponse)
    }

    def "returns bad request when authentication fails"() {
        given:
        def loginRequest = new LoginRequest("chef", "wrong")
        authService.authenticateUser(_ as LoginRequest) >> { throw new RuntimeException("bad credentials") }

        when:
        def response = authController.authenticateUser(loginRequest)

        then:
        response.statusCode.value() == 400
        !response.body.success
        response.body.message == "Invalid username or password!"
    }

    def "registers user successfully"() {
        given:
        def registerRequest = new RegisterRequest("chef", "chef@example.com", "password123")

        when:
        def response = authController.registerUser(registerRequest)

        then:
        1 * authService.registerUser(registerRequest)
        response.statusCode.value() == 200
        response.body.success
        response.body.message == "User registered successfully!"
    }

    def "returns bad request when registration fails"() {
        given:
        def registerRequest = new RegisterRequest("chef", "chef@example.com", "password123")
        authService.registerUser(registerRequest) >> { throw new RuntimeException("Error: Username is already taken!") }

        when:
        def response = authController.registerUser(registerRequest)

        then:
        response.statusCode.value() == 400
        !response.body.success
        response.body.message == "Error: Username is already taken!"
    }
}

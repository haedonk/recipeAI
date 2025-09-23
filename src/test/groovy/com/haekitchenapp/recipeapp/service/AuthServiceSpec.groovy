package com.haekitchenapp.recipeapp.service

import com.haekitchenapp.recipeapp.entity.User
import com.haekitchenapp.recipeapp.model.response.auth.LoginRequest
import com.haekitchenapp.recipeapp.model.response.auth.RegisterRequest
import com.haekitchenapp.recipeapp.repository.UserRepository
import com.haekitchenapp.recipeapp.security.JwtUtils
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import spock.lang.Specification
import spock.lang.Subject

class AuthServiceSpec extends Specification {

    AuthenticationManager authenticationManager = Mock()
    UserRepository userRepository = Mock()
    PasswordEncoder encoder = Mock()
    JwtUtils jwtUtils = Mock()

    @Subject
    AuthService authService = new AuthService(authenticationManager, userRepository, encoder, jwtUtils)

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "authenticateUser returns token when authentication succeeds"() {
        given:
        def loginRequest = new LoginRequest("chef", "secret")
        Authentication authentication = Mock()
        def principal = new User("chef", "chef@example.com", "encoded")

        when:
        def response = authService.authenticateUser(loginRequest)

        then:
        1 * authenticationManager.authenticate({ Authentication token ->
            token instanceof UsernamePasswordAuthenticationToken &&
                token.principal == loginRequest.username &&
                token.credentials == loginRequest.password
        }) >> authentication
        1 * jwtUtils.generateJwtToken(authentication) >> "jwt-token"
        1 * authentication.getPrincipal() >> principal
        0 * _

        response.token == "jwt-token"
        response.type == "Bearer"
    }

    def "authenticateUser propagates failures without touching repositories"() {
        given:
        def loginRequest = new LoginRequest("chef", "bad-password")
        def failure = new RuntimeException("authentication failed")

        when:
        authService.authenticateUser(loginRequest)

        then:
        def thrownException = thrown(RuntimeException)
        thrownException.is(failure)

        1 * authenticationManager.authenticate(_ as UsernamePasswordAuthenticationToken) >> { throw failure }
        0 * jwtUtils._
        0 * userRepository._
        0 * encoder._
    }

    def "registerUser rejects duplicate usernames"() {
        given:
        def request = new RegisterRequest("chef", "chef@example.com", "secret")

        when:
        authService.registerUser(request)

        then:
        def exception = thrown(RuntimeException)
        exception.message == "Error: Username is already taken!"

        1 * userRepository.existsByUsername("chef") >> true
        0 * userRepository._
        0 * encoder._
        0 * jwtUtils._
        0 * authenticationManager._
    }

    def "registerUser rejects duplicate emails"() {
        given:
        def request = new RegisterRequest("chef", "chef@example.com", "secret")

        when:
        authService.registerUser(request)

        then:
        def exception = thrown(RuntimeException)
        exception.message == "Error: Email is already in use!"

        1 * userRepository.existsByUsername("chef") >> false
        1 * userRepository.existsByEmail("chef@example.com") >> true
        0 * userRepository.save(_)
        0 * encoder._
        0 * jwtUtils._
        0 * authenticationManager._
    }

    def "registerUser encodes password and saves the new user"() {
        given:
        def request = new RegisterRequest("chef", "chef@example.com", "secret")

        when:
        authService.registerUser(request)

        then:
        1 * userRepository.existsByUsername("chef") >> false
        1 * userRepository.existsByEmail("chef@example.com") >> false
        1 * encoder.encode("secret") >> "encoded-secret"
        1 * userRepository.save({ User user ->
            user.username == "chef" &&
                user.email == "chef@example.com" &&
                user.password == "encoded-secret"
        }) >> { User saved -> saved }
        0 * _
    }

    def "registerUser surfaces repository save failures"() {
        given:
        def request = new RegisterRequest("chef", "chef@example.com", "secret")

        when:
        authService.registerUser(request)

        then:
        def exception = thrown(RuntimeException)
        exception.message == "Error: Failed to register user - database down"

        1 * userRepository.existsByUsername("chef") >> false
        1 * userRepository.existsByEmail("chef@example.com") >> false
        1 * encoder.encode("secret") >> "encoded-secret"
        1 * userRepository.save(_ as User) >> { throw new RuntimeException("database down") }
        0 * _
    }
}

package com.haekitchenapp.recipeapp.controller

import com.haekitchenapp.recipeapp.model.request.email.VerifyEmailRequestDto
import com.haekitchenapp.recipeapp.model.response.ApiResponse
import com.haekitchenapp.recipeapp.service.JwtTokenService
import com.haekitchenapp.recipeapp.service.UserService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class UserControllerSpec extends Specification {

    UserController userController
    UserService userService
    JwtTokenService jwtTokenService

    def setup() {
        userService = Mock(UserService)
        jwtTokenService = Mock(JwtTokenService)
        userController = new UserController(userService, jwtTokenService)
    }

    def "returns verification status for the user resolved from the JWT"() {
        given:
        HttpServletRequest request = Mock()

        when:
        def response = userController.isEmailVerified(request)

        then:
        1 * jwtTokenService.getUserIdFromRequest(request) >> 42L
        1 * userService.isUserEmailVerified(42L) >> ResponseEntity.ok(ApiResponse.success('Verification status retrieved', true))
        response.statusCode.value() == 200
        response.body.success
        response.body.message == 'Verification status retrieved'
        response.body.data == Boolean.TRUE
    }

    def "propagates failed verification status responses from the service"() {
        given:
        HttpServletRequest request = Mock()

        when:
        def response = userController.isEmailVerified(request)

        then:
        1 * jwtTokenService.getUserIdFromRequest(request) >> 77L
        1 * userService.isUserEmailVerified(77L) >> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error('Unable to determine verification status'))
        response.statusCode == HttpStatus.BAD_REQUEST
        !response.body.success
        response.body.message == 'Unable to determine verification status'
    }

    def "verifies email and returns a success response"() {
        given:
        def dto = new VerifyEmailRequestDto(5L, 'code-123')

        when:
        def response = userController.verifyEmail(dto)

        then:
        1 * userService.verifyEmail(dto)
        response.statusCode == HttpStatus.OK
        response.body.success
        response.body.message == 'Email verified successfully'
    }

    def "propagates exceptions thrown during email verification"() {
        given:
        def dto = new VerifyEmailRequestDto(6L, 'code-456')

        when:
        userController.verifyEmail(dto)

        then:
        1 * userService.verifyEmail(dto) >> { throw new IllegalArgumentException('Invalid verification code') }
        def ex = thrown(IllegalArgumentException)
        ex.message == 'Invalid verification code'
    }

    def "resends verification email for the user resolved from the request"() {
        given:
        HttpServletRequest request = Mock()

        when:
        def response = userController.resendVerificationEmail(request)

        then:
        1 * jwtTokenService.getUserIdFromRequest(request) >> 81L
        1 * userService.resendVerificationEmail(81L) >> ResponseEntity.ok(ApiResponse.success('Verification email sent'))
        response.statusCode.value() == 200
        response.body.success
        response.body.message == 'Verification email sent'
    }

    def "surfaces resend verification failures returned by the service"() {
        given:
        HttpServletRequest request = Mock()

        when:
        def response = userController.resendVerificationEmail(request)

        then:
        1 * jwtTokenService.getUserIdFromRequest(request) >> 82L
        1 * userService.resendVerificationEmail(82L) >> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error('Unable to send verification email'))
        response.statusCode == HttpStatus.BAD_REQUEST
        !response.body.success
        response.body.message == 'Unable to send verification email'
    }
}

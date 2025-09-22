package com.haekitchenapp.recipeapp.controller

import com.haekitchenapp.recipeapp.exception.EmbedFailureException
import com.haekitchenapp.recipeapp.exception.InvalidCredentialsException
import com.haekitchenapp.recipeapp.exception.InvalidValidationCodeException
import com.haekitchenapp.recipeapp.exception.RecipeNotFoundException
import com.haekitchenapp.recipeapp.exception.UserEmailExistsException
import com.haekitchenapp.recipeapp.exception.UserNotFoundException
import com.haekitchenapp.recipeapp.model.response.ApiResponse
import org.springframework.core.MethodParameter
import org.springframework.mail.MailAuthenticationException
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import spock.lang.Specification

class AdviceSpec extends Specification {

    Advice advice

    def setup() {
        advice = new Advice()
    }

    def "handleNotFound returns 404 and propagates message"() {
        when:
        def response = advice.handleNotFound(new RecipeNotFoundException("Recipe missing"))

        then:
        response.statusCode.value() == 404
        !response.body.success
        response.body.message == "Recipe missing"
    }

    def "handleEmbedFailure returns 500 with prefixed message"() {
        when:
        def response = advice.handleEmbedFailure(new EmbedFailureException("Vectorization broke"))

        then:
        response.statusCode.value() == 500
        response.body.message == "Embedding failed: Vectorization broke"
    }

    def "handleUserEmailExists returns conflict status"() {
        when:
        def response = advice.handleUserEmailExists(new UserEmailExistsException("Email already registered"))

        then:
        response.statusCode.value() == 409
        response.body.message == "Email already registered"
    }

    def "handleInvalidCredentials returns unauthorized status"() {
        when:
        def response = advice.handleInvalidCredentials(new InvalidCredentialsException("Bad credentials"))

        then:
        response.statusCode.value() == 401
        response.body.message == "Bad credentials"
    }

    def "handleInvalidValidationCode returns bad request"() {
        when:
        def response = advice.handleInvalidCredentials(new InvalidValidationCodeException("Code expired"))

        then:
        response.statusCode.value() == 400
        response.body.message == "Code expired"
    }

    def "handleUserNotFound returns 404"() {
        when:
        def response = advice.handleUserNotFound(new UserNotFoundException("User missing"))

        then:
        response.statusCode.value() == 404
        response.body.message == "User missing"
    }

    def "handleTypeMismatch formats parameter message"() {
        given:
        def exception = new MethodArgumentTypeMismatchException(
                "abc",
                Integer,
                "id",
                null,
                new IllegalArgumentException("bad format")
        )

        when:
        def response = advice.handleTypeMismatch(exception)

        then:
        response.statusCode.value() == 400
        response.body.message == "Invalid value 'abc' for parameter 'id'. Please provide a valid number."
    }

    def "handleValidationExceptions aggregates field errors"() {
        given:
        def bindingResult = Mock(BindingResult) {
            getFieldErrors() >> [
                    new FieldError("object", "title", "must not be blank"),
                    new FieldError("object", "servings", "must be positive")
            ]
        }
        def exception = new MethodArgumentNotValidException(Mock(MethodParameter), bindingResult)

        when:
        def response = advice.handleValidationExceptions(exception)

        then:
        response.statusCode.value() == 400
        response.body.message == "Validation failed: title: must not be blank, servings: must be positive"
    }

    def "handleIllegalArgument wraps IllegalArgumentException"() {
        when:
        def response = advice.handleIllegalArgument(new IllegalArgumentException("wrong state"))

        then:
        response.statusCode.value() == 400
        response.body.message == "Invalid request: wrong state"
    }

    def "handleIllegalArgument wraps NumberFormatException"() {
        when:
        def response = advice.handleIllegalArgument(new NumberFormatException("no number"))

        then:
        response.statusCode.value() == 400
        response.body.message == "Invalid request: no number"
    }

    def "handleMailAuthenticationException returns server error"() {
        when:
        def response = advice.handleMailAuthenticationException(new MailAuthenticationException("SMTP down"))

        then:
        response.statusCode.value() == 500
        response.body.message == "Email service error: SMTP down"
    }

    def "handleGenericException falls back to unexpected error message"() {
        when:
        def response = advice.handleGenericException(new Exception("boom"))

        then:
        response.statusCode.value() == 500
        response.body instanceof ApiResponse
        response.body.message == "An unexpected error occurred: boom"
    }
}

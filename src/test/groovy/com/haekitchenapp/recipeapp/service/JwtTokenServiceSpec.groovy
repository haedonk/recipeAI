package com.haekitchenapp.recipeapp.service

import com.haekitchenapp.recipeapp.security.JwtUtils
import jakarta.servlet.http.HttpServletRequest
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

@Subject(JwtTokenService)
class JwtTokenServiceSpec extends Specification {

    JwtUtils jwtUtils = Mock()
    UserService userService = Mock()
    JwtTokenService jwtTokenService
    HttpServletRequest request

    def setup() {
        jwtTokenService = new JwtTokenService(jwtUtils, userService)
        request = Mock(HttpServletRequest)
    }

    def "getUserIdFromRequest extracts decoded user id from bearer token"() {
        when:
        Long result = jwtTokenService.getUserIdFromRequest(request)

        then:
        1 * request.getHeader("Authorization") >> "Bearer encoded-user"
        1 * jwtUtils.getUserIdFromJwtToken("encoded-user") >> 101L
        result == 101L
    }

    @Unroll
    def "getUserIdFromRequest returns null when Authorization header is #description"() {
        when:
        Long result = jwtTokenService.getUserIdFromRequest(request)

        then:
        1 * request.getHeader("Authorization") >> headerValue
        0 * jwtUtils._
        result == null

        where:
        description              | headerValue
        "missing"               | null
        "blank"                 | "   "
        "using alternate scheme"| "Token token-value"
    }
}

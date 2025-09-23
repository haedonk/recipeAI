package com.haekitchenapp.recipeapp.service.rateLimit

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.PrintWriter
import java.io.StringWriter
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class RateLimitFilterSpec extends Specification {

    RateLimiterService rateLimiterService = Mock()

    @Subject
    RateLimitFilter filter = new RateLimitFilter(rateLimiterService)

    HttpServletRequest request = Mock()
    HttpServletResponse response = Mock()
    FilterChain chain = Mock()

    def setup() {
        filter.@activeProfile = "default"
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    def "local profile bypasses rate limiting and continues filter chain"() {
        given:
        filter.@activeProfile = "local"

        when:
        filter.doFilter(request, response, chain)

        then:
        1 * chain.doFilter(request, response)
        0 * rateLimiterService._
    }

    def "public endpoints use IP based rate limiting thresholds"() {
        given:
        request.getRequestURI() >> "/public/recipes"
        request.getRemoteAddr() >> "10.0.0.1"

        when:
        filter.doFilter(request, response, chain)

        then:
        1 * rateLimiterService.isAllowed("PUBLIC:10.0.0.1", 30, 100) >> true
        1 * chain.doFilter(request, response)
        0 * response.setStatus(_)
    }

    def "authenticated requests use user specific rate limiting thresholds"() {
        given:
        request.getRequestURI() >> "/api/recipes"
        request.getRemoteAddr() >> "10.0.0.1"
        Authentication authentication = Mock() {
            isAuthenticated() >> true
            getPrincipal() >> "userPrincipal"
            getName() >> "user-123"
        }
        def context = SecurityContextHolder.createEmptyContext()
        context.authentication = authentication
        SecurityContextHolder.setContext(context)

        when:
        filter.doFilter(request, response, chain)

        then:
        1 * rateLimiterService.isAllowed("USER:user-123", 100, 1000) >> true
        1 * chain.doFilter(request, response)
        0 * response.setStatus(_)
    }

    def "non public unauthenticated requests fall back to IP auth key"() {
        given:
        request.getRequestURI() >> "/secure/recipes"
        request.getRemoteAddr() >> "10.0.0.2"
        SecurityContextHolder.clearContext()

        when:
        filter.doFilter(request, response, chain)

        then:
        1 * rateLimiterService.isAllowed("AUTH:10.0.0.2", 10, 30) >> true
        1 * chain.doFilter(request, response)
        0 * response.setStatus(_)
    }

    def "when rate limiter denies request a 429 response is returned"() {
        given:
        request.getRequestURI() >> "/public/limited"
        request.getRemoteAddr() >> "203.0.113.5"
        StringWriter stringWriter = new StringWriter()

        when:
        filter.doFilter(request, response, chain)

        then:
        1 * rateLimiterService.isAllowed("PUBLIC:203.0.113.5", 30, 100) >> false
        1 * response.setStatus(429)
        1 * response.getWriter() >> new PrintWriter(stringWriter)
        0 * chain.doFilter(_, _)
        stringWriter.toString() == "Rate limit exceeded. Please create an account for unlimited access."
    }

    @Unroll
    def "getUserId returns 'unknown' when authentication is #scenario"() {
        given:
        contextConfigurer.call()

        when:
        def method = RateLimitFilter.getDeclaredMethod("getUserId")
        method.accessible = true
        def result = method.invoke(filter)

        then:
        result == "unknown"

        where:
        scenario                            | contextConfigurer
        "missing from security context"     | { SecurityContextHolder.clearContext() }
        "throwing while accessing context" | {
            SecurityContext securityContext = Mock() {
                1 * getAuthentication() >> { throw new RuntimeException("failure") }
            }
            SecurityContextHolder.setContext(securityContext)
        }
    }
}

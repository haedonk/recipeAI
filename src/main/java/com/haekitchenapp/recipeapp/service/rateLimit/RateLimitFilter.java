package com.haekitchenapp.recipeapp.service.rateLimit;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

@Component
public class RateLimitFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimiterService rateLimiterService;

    @Value("${rate.limit.excluded.paths:}")
    private List<String> excludedPaths;

    public RateLimitFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();

        // Skip rate limiting for excluded paths
        if (isExcludedPath(path)) {
            logger.debug("Skipping rate limit for excluded path: {}", path);
            chain.doFilter(request, response);
            return;
        }

        // Skip rate limiting for authenticated users
        if (isAuthenticated()) {
            logger.debug("Skipping rate limit for authenticated user");
            chain.doFilter(request, response);
            return;
        }

        // Apply rate limiting only for non-authenticated users
        String ip = req.getRemoteAddr();
        if (!rateLimiterService.isAllowed(ip)) {
            logger.debug("Rate limit exceeded for IP: {}", ip);

            // Determine if this is a login attempt or path that should get special handling
            String requestPath = req.getRequestURI();
            boolean isAuthRequest = requestPath.contains("/auth/") ||
                                    requestPath.contains("/login") ||
                                    requestPath.contains("/api/users/signin");

            // If this is a login attempt, slightly decrease their rate count to give them a chance
            if (isAuthRequest) {
                // Reduce by 5 requests to give them some breathing room for login
                rateLimiterService.decreaseRateCount(ip, 5);
                logger.debug("Decreased rate count for authentication attempt from IP: {}", ip);
            } else {
                // For non-auth requests, just decrease by 1 to be slightly forgiving
                rateLimiterService.decreaseRateCount(ip, 1);
            }

            res.setStatus(429);
            res.getWriter().write("Rate limit exceeded. Please create an account for unlimited access.");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isExcludedPath(String path) {
        if (excludedPaths == null || excludedPaths.isEmpty()) {
            return false;
        }
        return excludedPaths.stream().anyMatch(path::startsWith);
    }

    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() &&
               !"anonymousUser".equals(authentication.getPrincipal().toString());
    }
}

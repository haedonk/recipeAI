package com.haekitchenapp.recipeapp.service.rateLimit;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class RateLimitFilter implements Filter {

    private final RateLimiterService rateLimiterService;

    @Value("${rate.limit.excluded.paths:}")
    private List<String> excludedPaths;

    public RateLimitFilter(RateLimiterService rateLimiterService, List<String> excludedPaths) {
        this.rateLimiterService = rateLimiterService;
        this.excludedPaths = excludedPaths;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String path = req.getRequestURI();

        // Skip rate limiting for excluded paths
        if (isExcludedPath(path)) {
            chain.doFilter(request, response);
            return;
        }

        String ip = req.getRemoteAddr();

        // Skip if authenticated (optional, can check token or user context)
        boolean isAuthenticated = req.getUserPrincipal() != null;
        if (!isAuthenticated && !rateLimiterService.isAllowed(ip)) {
            res.setStatus(429);
            res.getWriter().write("Rate limit exceeded. Please create an account to continue.");
            return;
        }

        chain.doFilter(request, response);
    }
    private boolean isExcludedPath(String path) {
        return excludedPaths.stream().anyMatch(path::startsWith);
    }
}

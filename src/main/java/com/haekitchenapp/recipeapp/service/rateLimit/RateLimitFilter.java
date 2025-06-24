package com.haekitchenapp.recipeapp.service.rateLimit;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RateLimitFilter implements Filter {

    private final RateLimiterService rateLimiterService;

    public RateLimitFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

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
}

package com.haekitchenapp.recipeapp.service.rateLimit;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RateLimitFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimiterService rateLimiterService;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    public RateLimitFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Skip rate limiting if environment is local
        if ("local".equals(activeProfile)) {
            logger.debug("Rate limiting disabled for local environment");
            chain.doFilter(request, response);
            return;
        }

        String path = req.getRequestURI();
        String ip = req.getRemoteAddr();
        boolean allowed;

        if (isPublicPath(path)) {
            // Public endpoints → use IP-based rate limiting
            allowed = rateLimiterService.isAllowed("PUBLIC:" + ip, 30, 100);
        } else if (isAuthenticated()) {
            // Authenticated endpoints → use user ID for rate limiting
            String userId = getUserId();
            allowed = rateLimiterService.isAllowed("USER:" + userId, 100, 1000);
        } else {
            // Non-authenticated, non-public → strict IP-based rate limiting
            allowed = rateLimiterService.isAllowed("AUTH:" + ip, 10, 30);
        }

        if (!allowed) {
            res.setStatus(429);
            res.getWriter().write("Rate limit exceeded. Please create an account for unlimited access.");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return path != null && path.startsWith("/public/");
    }

    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal().toString());
    }

    private String getUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() != null) {
                return authentication.getName(); // usually the username or user ID
            }
        } catch (Exception e) {
            logger.warn("Failed to extract userId from authentication", e);
        }
        return "unknown";
    }
}
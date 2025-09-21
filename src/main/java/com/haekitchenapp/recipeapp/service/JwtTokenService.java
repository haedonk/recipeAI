package com.haekitchenapp.recipeapp.service;

import com.haekitchenapp.recipeapp.security.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtUtils jwtUtils;
    private final UserService userService;

    public Long getUserIdFromRequest(HttpServletRequest request) {
        String jwt = parseJwtFromRequest(request);
        if (jwt != null) {
            return jwtUtils.getUserIdFromJwtToken(jwt);
        }
        return null;
    }

    private String parseJwtFromRequest(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}

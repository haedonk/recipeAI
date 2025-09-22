package com.haekitchenapp.recipeapp.model.response.auth;

import lombok.Data;

@Data
public class JwtResponse {
    // Getters and setters
    private String token;
    private String type = "Bearer";

    public JwtResponse(String accessToken) {
        this.token = accessToken;
    }
}

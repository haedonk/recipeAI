package com.haekitchenapp.recipeapp.model.request.togetherAi;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoleContent {
    String role;
    String content;

    public static RoleContent getUserRole(String content) {
        return new RoleContent("user", content);
    }
}

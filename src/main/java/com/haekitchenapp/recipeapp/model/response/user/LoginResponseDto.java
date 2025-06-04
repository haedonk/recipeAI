package com.haekitchenapp.recipeapp.model.response.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponseDto {
    private Long userId;
    private String email;
    private String username;
}

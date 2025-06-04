package com.haekitchenapp.recipeapp.model.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDto {
    private String email;
    private String username;
    @NotBlank
    private String password;
}

package com.haekitchenapp.recipeapp.model.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRequestDto {
    private String email;
    @NotBlank
    private String password;
}

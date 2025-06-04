package com.haekitchenapp.recipeapp.model.request.email;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VerifyEmailRequestDto {
    @NotNull
    private Long userId;
    @NotBlank
    private String verificationCode;
}

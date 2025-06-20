package com.haekitchenapp.recipeapp.model.request.togetherAi;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoleContentDto {
    @NotBlank
    String role;
    @NotBlank
    String content;

    public RoleContent toRoleContent() {
        return new RoleContent(role, content);
    }
}

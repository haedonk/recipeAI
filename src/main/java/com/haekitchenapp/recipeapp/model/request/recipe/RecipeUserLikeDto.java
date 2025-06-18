package com.haekitchenapp.recipeapp.model.request.recipe;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecipeUserLikeDto {
    @JsonProperty("user_id")
    @NotNull
    private Long userId;

    @JsonProperty("recipe_id")
    @NotNull
    private Long recipeId;
}

package com.haekitchenapp.recipeapp.model.request.recipe;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RecipeUserLikeDto {
    @JsonProperty("recipe_id")
    @NotNull
    private Long recipeId;
}

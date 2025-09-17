package com.haekitchenapp.recipeapp.model.response.recipe;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeAISkeletonId extends RecipeAISkeleton{
    @NotNull
    private Long id;
    private String userPrompt;
}

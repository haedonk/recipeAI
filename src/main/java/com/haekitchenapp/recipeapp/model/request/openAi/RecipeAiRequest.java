package com.haekitchenapp.recipeapp.model.request.openAi;

import com.haekitchenapp.recipeapp.model.request.recipe.RecipeIngredientRequest;
import com.haekitchenapp.recipeapp.model.response.recipe.RecipeAISkeleton;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeAiRequest {
    private String systemPrompt;

    private RecipeAISkeleton userMessage;
}

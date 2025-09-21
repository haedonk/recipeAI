package com.haekitchenapp.recipeapp.model.request.openAi;

import com.haekitchenapp.recipeapp.model.response.recipe.RecipeAISkeleton;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeAiRequest {
    private String systemPrompt;

    private RecipeAISkeleton userMessage;
}

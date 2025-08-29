package com.haekitchenapp.recipeapp.model.response.recipe;

import com.haekitchenapp.recipeapp.entity.Recipe;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RecipeDetailsDto {

    public RecipeDetailsDto(String title, List<String> ingredients, String instructions) {
        this.title = title;
        this.instructions = instructions;
        this.ingredients = ingredients;
        this.embedSummary = getFullSummary(instructions);
    }

    private String title;
    private List<String> ingredients;
    private String instructions;
    private String embedSummary;

    public String getFullSummary(String summary) {
        return "Title: " + this.title
                + "\n Ingredients: " + this.ingredients.stream().reduce((a, b) -> a + ", " + b).orElse("No ingredients")
                + "\n Instructions: " + summary;
    }
}
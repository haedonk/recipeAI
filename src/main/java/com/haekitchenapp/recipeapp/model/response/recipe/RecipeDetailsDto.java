package com.haekitchenapp.recipeapp.model.response.recipe;

import lombok.Getter;

import java.util.List;

@Getter
public class RecipeDetailsDto {

    public RecipeDetailsDto(String title, List<String> ingredients, String instructions) {
        this(title, ingredients, List.of(), instructions, null);
    }

    public RecipeDetailsDto(String title, List<String> ingredients, List<String> cuisines, String instructions, Long id) {
        this.id = id;
        this.title = title != null ? title : "";
        this.instructions = instructions != null ? instructions : "";
        this.cuisines = cuisines != null ? cuisines : List.of();
        this.ingredients = ingredients != null ? ingredients : List.of();
        this.embedSummary = getFullSummary(this.instructions);
    }

    private Long id;
    private String title;
    private List<String> cuisines;
    private List<String> ingredients;
    private String instructions;
    private String embedSummary;

    public String getFullSummary(String summary) {
        String ingredientSummary = ingredients == null || ingredients.isEmpty()
                ? "No ingredients"
                : String.join(", ", ingredients);
        String instructionsSummary = summary != null ? summary : "";

        return "Title: " + (title != null ? title : "")
                + "\n Ingredients: " + ingredientSummary
                + "\n Instructions: " + instructionsSummary;
    }
}
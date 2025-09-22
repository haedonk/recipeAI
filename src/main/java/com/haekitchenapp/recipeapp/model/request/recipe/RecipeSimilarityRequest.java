package com.haekitchenapp.recipeapp.model.request.recipe;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeSimilarityRequest {

    public RecipeSimilarityRequest(String prompt){
        this.prompt = prompt;
        this.excludeIngredients = "";
    }

    private String title;

    private String prompt;

    private String cuisine;

    private String includeIngredients;

    private String excludeIngredients;

    private String mealType;

    private String detailLevel;

    @NotNull
    private Integer limit;

    public boolean isPromptBased(){
        this.prompt = this.prompt != null ? this.prompt.trim() : null;
        return this.prompt != null && !this.prompt.isEmpty();
    }

    public boolean isValidFullRequest(){
        this.title = this.title != null ? this.title.trim() : null;
        this.cuisine = this.cuisine != null ? this.cuisine.trim() : null;
        this.includeIngredients = this.includeIngredients != null ? this.includeIngredients.trim() : null;
        this.excludeIngredients = this.excludeIngredients != null ? this.excludeIngredients.trim() : null;
        this.mealType = this.mealType != null ? this.mealType.trim() : null;
        this.detailLevel = this.detailLevel != null ? this.detailLevel.trim() : null;
        return (this.title != null && !this.title.isEmpty()) ||
               (this.cuisine != null && !this.cuisine.isEmpty()) ||
               (this.includeIngredients != null && !this.includeIngredients.isEmpty()) ||
               (this.excludeIngredients != null && !this.excludeIngredients.isEmpty()) ||
               (this.mealType != null && !this.mealType.isEmpty()) ||
               (this.detailLevel != null && !this.detailLevel.isEmpty());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if(title != null) {
            sb.append("Title: ").append(title).append(", ");
        }
        if(cuisine != null) {
            sb.append("Cuisine: ").append(cuisine).append(", ");
        }
        if(includeIngredients != null) {
            sb.append("Include Ingredients: ").append(includeIngredients).append(", ");
        }
        if(excludeIngredients != null) {
            sb.append("Exclude Ingredients: ").append(excludeIngredients).append(", ");
        }
        if(mealType != null) {
            sb.append("Meal Type: ").append(mealType).append(", ");
        }
        if(detailLevel != null) {
            sb.append("Detail Level: ").append(detailLevel);
        }
        return sb.toString();
    }
}

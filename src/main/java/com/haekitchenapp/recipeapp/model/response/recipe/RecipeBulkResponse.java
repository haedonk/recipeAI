package com.haekitchenapp.recipeapp.model.response.recipe;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeBulkResponse {
    List<RecipeResponse> recipe;
    boolean isLastPage;
}

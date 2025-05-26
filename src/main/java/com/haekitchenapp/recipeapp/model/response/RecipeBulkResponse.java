package com.haekitchenapp.recipeapp.model.response;

import com.haekitchenapp.recipeapp.model.request.RecipeIngredientRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeBulkResponse {
    List<RecipeResponse> recipe;
    boolean isLastPage;
}

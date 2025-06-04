package com.haekitchenapp.recipeapp.model.response.recipe;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RecipeDuplicatesByTitleResponse {

    private List<RecipeDuplicatesByTitleDto> duplicates;

    boolean isLastPage;
}

package com.haekitchenapp.recipeapp.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecipeDuplicatesByTitleDto {

    private String title;
    private Long count;

}

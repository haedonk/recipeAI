package com.haekitchenapp.recipeapp.model.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecipeTitleDto {
    private Long id;
    private String title;

}

package com.haekitchenapp.recipeapp.model.response.recipe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecipeTitleDto {

    public RecipeTitleDto(Long id, String title) {
        this.id = id;
        this.title = title;
    }

    private Long id;
    private String title;
    private String instructions;

}

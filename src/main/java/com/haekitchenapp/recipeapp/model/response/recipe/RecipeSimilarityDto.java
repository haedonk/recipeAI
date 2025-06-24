package com.haekitchenapp.recipeapp.model.response.recipe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RecipeSimilarityDto {

    private Long id;
    private String title;
    private String summary;
    private Double similarity;

}

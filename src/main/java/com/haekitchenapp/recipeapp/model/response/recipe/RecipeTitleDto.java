package com.haekitchenapp.recipeapp.model.response.recipe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String instructions;

}

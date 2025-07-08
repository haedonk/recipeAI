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

    public RecipeSimilarityDto(Long id, String title, String summary, Double similarity) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.similarity = similarity;
    }

    private Long id;
    private String title;
    private String summary;
    private Double similarity;
    private int titleSimilarityRank = 0;
    private int similarityRank = 0;
    private int includesIngredientsCount = 0;

    @Override
    public String toString() {
        return "RecipeSimilarityDto{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", summary='" + summary + '\'' +
                ", similarity=" + similarity +
                ", similarityRank=" + similarityRank +
                ", includesIngredientsCount=" + includesIngredientsCount +
                '}';
    }
}

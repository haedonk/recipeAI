package com.haekitchenapp.recipeapp.model.response.recipe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

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

    public RecipeSimilarityDto(RecipeSimilarityView view) {
        this.id = view.getId();
        this.title = view.getTitle();
        this.summary = view.getSummary();
        this.similarity = view.getSimilarity();
        this.cosineDistance = view.getCosineDistance();
    }

    private Long id;
    private String title;
    private String summary;
    private List<String> cuisines;
    private Double similarity;
    private Double cosineDistance;
    private Double percentSimilarity;
    private int titleSimilarityRank = 0;
    private int similarityRank = 0;
    private int cuisineMatchRank = 0;
    private int includesIngredientsCount = 0;
    private boolean exactTitleMatch;
    private boolean prefixTitleMatch;

    @Override
    public String toString() {
        return "RecipeSimilarityDto{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", summary='" + summary.substring(0, 25) + "..." + '\'' +
                ", cuisine='" + cuisines + '\'' +
                ", similarity=" + similarity +
                ", percentSimilarity=" + percentSimilarity +
                ", titleSimilarityRank=" + titleSimilarityRank +
                ", similarityRank=" + similarityRank +
                ", cuisineMatchRank=" + cuisineMatchRank +
                ", includesIngredientsCount=" + includesIngredientsCount +
                '}';
    }
}

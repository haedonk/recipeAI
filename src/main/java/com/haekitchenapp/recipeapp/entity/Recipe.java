package com.haekitchenapp.recipeapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@Table(name = "recipes")
public class Recipe extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Transient
    private Double[] embedding;

    private Integer prepTime;
    private Integer cookTime;
    private Integer servings;
    private Long createdBy;
    private Boolean reprocessed;
    @Column(updatable = false)
    private Boolean aiGenerated = false;
    private Long cleanedFrom;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<RecipeIngredient> ingredients;

    @JsonIgnore
    public String getEmbedString(){
        if (embedding == null || embedding.length == 0) {
            return null;
        }
        return Arrays.stream(embedding)
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));
    }

    @Override
    public String toString() {
        return "Recipe{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", instructions='" + instructions + '\'' +
                ", summary='" + summary + '\'' +
                ", prepTime=" + prepTime +
                ", cookTime=" + cookTime +
                ", servings=" + servings +
                ", createdBy=" + createdBy +
                '}';
    }
}

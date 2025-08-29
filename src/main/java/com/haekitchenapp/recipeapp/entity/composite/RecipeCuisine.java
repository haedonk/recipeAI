package com.haekitchenapp.recipeapp.entity.composite;

import com.haekitchenapp.recipeapp.entity.BaseEntity;
import com.haekitchenapp.recipeapp.entity.Cuisine;
import com.haekitchenapp.recipeapp.entity.Recipe;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "recipe_cuisine")
public class RecipeCuisine  extends BaseEntity {

    @EmbeddedId
    private RecipeCuisineId id;

    @ManyToOne
    @MapsId("recipeId")
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @ManyToOne
    @MapsId("cuisineId")
    @JoinColumn(name = "cuisine_id")
    private Cuisine cuisine;
}

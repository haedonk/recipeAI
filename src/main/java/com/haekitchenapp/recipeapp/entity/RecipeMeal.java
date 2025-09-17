package com.haekitchenapp.recipeapp.entity;

import com.haekitchenapp.recipeapp.entity.composite.RecipeMealId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "recipe_meal")
public class RecipeMeal extends BaseEntity {

    @EmbeddedId
    private RecipeMealId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("recipeId")
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("mealTypeId")
    @JoinColumn(name = "meal_type_id", nullable = false)
    private MealType mealType;
}

package com.haekitchenapp.recipeapp.entity.composite;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RecipeMealId implements Serializable {

    @Column(name = "recipe_id")
    private Long recipeId;

    @Column(name = "meal_type_id")
    private Short mealTypeId;
}

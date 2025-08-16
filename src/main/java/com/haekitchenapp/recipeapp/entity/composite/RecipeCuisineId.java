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
public class RecipeCuisineId implements Serializable {

    @Column(name = "recipe_id")
    private Long recipeId;

    @Column(name = "cuisine_id")
    private Integer cuisineId;
}

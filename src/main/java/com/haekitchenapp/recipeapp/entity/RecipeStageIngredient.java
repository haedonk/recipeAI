package com.haekitchenapp.recipeapp.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@Table(name = "recipe_staging_ingredients")
public class RecipeStageIngredient extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "recipe_id", nullable = false)
    @JsonBackReference
    private RecipeStage recipe;

    @ManyToOne
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    private String quantity;
    private Integer unitId;

    @JsonIgnore
    public static Set<RecipeIngredient> getRecipeIngredients(Set<RecipeStageIngredient> recipeStageIngredients,
                                                             Recipe recipe) {
        return recipeStageIngredients.stream().map(stageIngredient -> {
            RecipeIngredient recipeIngredient = new RecipeIngredient();
            recipeIngredient.setRecipe(recipe);
            recipeIngredient.setIngredient(stageIngredient.getIngredient());
            recipeIngredient.setQuantity(stageIngredient.getQuantity());
            recipeIngredient.setUnitId(stageIngredient.getUnitId());
            return recipeIngredient;
        }).collect(Collectors.toSet());
    }
}

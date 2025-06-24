package com.haekitchenapp.recipeapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "recipes_staging")
public class RecipeStage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    private Integer prepTime;
    private Integer cookTime;
    private Integer servings;
    private Long createdBy;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private Set<RecipeStageIngredient> ingredients;

    @JsonIgnore
    public Recipe getRecipe() {
        Recipe recipe = new Recipe();
        recipe.setId(this.id);
        recipe.setTitle(this.title);
        recipe.setInstructions(this.instructions);
        recipe.setPrepTime(this.prepTime);
        recipe.setCookTime(this.cookTime);
        recipe.setServings(this.servings);
        recipe.setCreatedBy(this.createdBy);
        recipe.setIngredients(RecipeStageIngredient.getRecipeIngredients(this.ingredients, recipe));
        return recipe;
    }

    @Override
    public String toString() {
        return "Recipe{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", instructions='" + instructions + '\'' +
                ", prepTime=" + prepTime +
                ", cookTime=" + cookTime +
                ", servings=" + servings +
                ", createdBy=" + createdBy +
                '}';
    }
}


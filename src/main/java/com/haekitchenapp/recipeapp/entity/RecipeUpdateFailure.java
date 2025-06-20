package com.haekitchenapp.recipeapp.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "recipe_update_failures")
@Data
public class RecipeUpdateFailure {

    public RecipeUpdateFailure(Long recipeId, String reason) {
        this.recipeId = recipeId;
        this.reason = reason;
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long recipeId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    private final LocalDateTime timestamp = LocalDateTime.now();

}

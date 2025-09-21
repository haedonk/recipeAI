package com.haekitchenapp.recipeapp.entity;

import com.haekitchenapp.recipeapp.entity.composite.RecipeLikesId;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "recipe_likes")
@Getter
@Setter
public class RecipeLikes {

    @EmbeddedId
    private RecipeLikesId id = new RecipeLikesId();

    @Column(name = "liked_at", nullable = false, updatable = false)
    private LocalDateTime likedAt;

    @PrePersist
    public void prePersist() {
        likedAt = LocalDateTime.now();
    }
}

package com.haekitchenapp.recipeapp.entity;

import com.haekitchenapp.recipeapp.entity.composite.RecipeCuisine;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Entity
@Getter
@Setter
@Table(name = "cuisine")
public class Cuisine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 100, nullable = false, unique = true)
    private String name;

    @OneToMany(mappedBy = "cuisine")
    private Set<RecipeCuisine> recipes;
}

package com.custom.recipe.ingredient.domain;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "ingredient_category")
@Getter
public class IngredientCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;
}

package com.custom.recipe.ingredient.domain;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "ingredient")
@Getter
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(name = "category_id")
    private Long categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_unit", nullable = false, length = 50)
    private UnitType defaultUnit;

    @Column(columnDefinition = "TEXT")
    private String description;

    protected Ingredient() {}

    public Ingredient(String name, Long categoryId, UnitType defaultUnit, String description) {
        this.name = name;
        this.categoryId = categoryId;
        this.defaultUnit = defaultUnit;
        this.description = description;
    }

    public void update(String name, Long categoryId, UnitType defaultUnit, String description) {
        this.name = name;
        this.categoryId = categoryId;
        this.defaultUnit = defaultUnit;
        this.description = description;
    }
}

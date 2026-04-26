package com.custom.recipe.ingredient.dto;

import com.custom.recipe.ingredient.domain.Ingredient;
import com.custom.recipe.ingredient.domain.UnitType;
import lombok.Getter;

@Getter
public class IngredientResponse {

    private final Long id;
    private final String name;
    private final Long categoryId;
    private final UnitType defaultUnit;
    private final String description;

    public IngredientResponse(Ingredient ingredient) {
        this.id = ingredient.getId();
        this.name = ingredient.getName();
        this.categoryId = ingredient.getCategoryId();
        this.defaultUnit = ingredient.getDefaultUnit();
        this.description = ingredient.getDescription();
    }
}

package com.custom.recipe.ingredient.dto;

import com.custom.recipe.ingredient.domain.UnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class IngredientRequest {

    @NotBlank
    private String name;

    @NotNull
    private Long categoryId;

    @NotNull
    private UnitType defaultUnit;

    private String description;
}

package com.custom.recipe.ingredient.repository;

import com.custom.recipe.ingredient.domain.IngredientCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientCategoryRepository extends JpaRepository<IngredientCategory, Long> {
}

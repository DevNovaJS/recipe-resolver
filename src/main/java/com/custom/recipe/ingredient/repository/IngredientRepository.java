package com.custom.recipe.ingredient.repository;

import com.custom.recipe.ingredient.domain.Ingredient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    Page<Ingredient> findAllByCategoryId(Long categoryId, Pageable pageable);
}

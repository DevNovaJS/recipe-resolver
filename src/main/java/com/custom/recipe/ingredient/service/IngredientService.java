package com.custom.recipe.ingredient.service;

import com.custom.recipe.ingredient.domain.Ingredient;
import com.custom.recipe.ingredient.dto.IngredientRequest;
import com.custom.recipe.ingredient.dto.IngredientResponse;
import com.custom.recipe.ingredient.repository.IngredientCategoryRepository;
import com.custom.recipe.ingredient.repository.IngredientRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientService {

    private final IngredientRepository ingredientRepository;
    private final IngredientCategoryRepository categoryRepository;

    @Transactional
    public IngredientResponse create(IngredientRequest request) {
        validateCategoryExists(request.getCategoryId());
        Ingredient ingredient = new Ingredient(
                request.getName(),
                request.getCategoryId(),
                request.getDefaultUnit(),
                request.getDescription()
        );
        return new IngredientResponse(ingredientRepository.save(ingredient));
    }

    public IngredientResponse findById(Long id) {
        return new IngredientResponse(getIngredient(id));
    }

    public Page<IngredientResponse> findAll(Long categoryId, Pageable pageable) {
        Page<Ingredient> page = (categoryId != null)
                ? ingredientRepository.findAllByCategoryId(categoryId, pageable)
                : ingredientRepository.findAll(pageable);
        return page.map(IngredientResponse::new);
    }

    @Transactional
    public IngredientResponse update(Long id, IngredientRequest request) {
        validateCategoryExists(request.getCategoryId());
        Ingredient ingredient = getIngredient(id);
        ingredient.update(request.getName(), request.getCategoryId(), request.getDefaultUnit(), request.getDescription());
        return new IngredientResponse(ingredient);
    }

    @Transactional
    public void delete(Long id) {
        Ingredient ingredient = getIngredient(id);
        ingredientRepository.delete(ingredient);
    }

    private Ingredient getIngredient(Long id) {
        return ingredientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("재료를 찾을 수 없습니다. id=" + id));
    }

    private void validateCategoryExists(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new IllegalArgumentException("존재하지 않는 카테고리입니다. categoryId=" + categoryId);
        }
    }
}

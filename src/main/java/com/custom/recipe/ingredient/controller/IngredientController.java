package com.custom.recipe.ingredient.controller;

import com.custom.recipe.ingredient.dto.IngredientRequest;
import com.custom.recipe.ingredient.dto.IngredientResponse;
import com.custom.recipe.ingredient.service.IngredientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/ingredients")
@RequiredArgsConstructor
public class IngredientController {

    private final IngredientService ingredientService;

    @PostMapping
    public ResponseEntity<IngredientResponse> create(@Valid @RequestBody IngredientRequest request) {
        IngredientResponse response = ingredientService.create(request);
        return ResponseEntity.created(URI.create("/ingredients/" + response.getId())).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IngredientResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ingredientService.findById(id));
    }

    @GetMapping
    public ResponseEntity<Page<IngredientResponse>> findAll(
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        return ResponseEntity.ok(ingredientService.findAll(categoryId, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<IngredientResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody IngredientRequest request
    ) {
        return ResponseEntity.ok(ingredientService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ingredientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

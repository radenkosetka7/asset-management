package com.example.asset_management.controller;

import com.example.asset_management.service.ICategoryAttributeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories/attributes")
@RequiredArgsConstructor
public class CategoryAttributeController {

    private final ICategoryAttributeService categoryAttributeService;

    @DeleteMapping("/{categoryId}/{attributeId}")
    public ResponseEntity<Void> deleteCategoryAttribute(@PathVariable Long categoryId, @PathVariable Long attributeId) {
        categoryAttributeService.delete(categoryId, attributeId);
        return ResponseEntity.noContent().build();
    }
}

package com.example.asset_management.dto.response;

import com.example.asset_management.model.Category;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.List;

public record CategoryResponse(Long id, String description, @Nullable Long parentCategoryId, String name,
                               @Nullable List<AttributeResponse> attributes) implements Serializable {

    public static CategoryResponse from(Category category, List<AttributeResponse> attributeResponse) {
        Category parent = category.getParentCategory();
        Long parentCategoryId = parent != null ? parent.getId() : null;
        return new CategoryResponse(category.getId(), category.getDescription(), parentCategoryId, category.getName(), attributeResponse);
    }

    public static CategoryResponse from(Category category) {
        Category parent = category.getParentCategory();
        Long parentCategoryId = parent != null ? parent.getId() : null;
        return new CategoryResponse(category.getId(), category.getDescription(), parentCategoryId, category.getName(), null);
    }
}

package com.example.asset_management.dto.request;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CategoryRequest(
        @NotBlank(message = "Description is required")
        String description,
        @Nullable Long parentCategoryId,
        @NotBlank(message = "Name is required")
        String name,
        @NotBlank(message = "Attributes are required")
        List<AttributeRequest> attributeRequestList) {
}

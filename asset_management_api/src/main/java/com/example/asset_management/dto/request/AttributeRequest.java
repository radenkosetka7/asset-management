package com.example.asset_management.dto.request;

import com.example.asset_management.dto.enumerator.AttributeDataType;
import jakarta.validation.constraints.NotBlank;

public record AttributeRequest(
        @NotBlank(message = "Name is required")
        String name,
        @NotBlank(message = "DataType is required")
        AttributeDataType dataType) {
}

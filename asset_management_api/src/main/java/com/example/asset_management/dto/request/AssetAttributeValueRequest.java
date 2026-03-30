package com.example.asset_management.dto.request;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AssetAttributeValueRequest(
        @NotBlank(message = "AttributeId is required")
        Long attributeId,
        @Nullable String valueString,
        @Nullable BigDecimal valueNumber,
        @Nullable LocalDate valueData,
        @Nullable Boolean valueBool) {
}

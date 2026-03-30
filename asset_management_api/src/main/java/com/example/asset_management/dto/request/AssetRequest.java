package com.example.asset_management.dto.request;

import com.example.asset_management.dto.enumerator.AssetStatus;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record AssetRequest(
        @NotBlank(message = "AssetCode is required")
        String assetCode,
        @NotBlank(message = "Name is required")
        String name,
        @NotBlank(message = "Description is required")
        String description,
        @Nullable AssetStatus assetStatus,
        @NotNull(message = "CategoryId is required")
        Long categoryId,
        @Nullable List<AssetAttributeValueRequest> assetAttributeValue) {
}

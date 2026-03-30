package com.example.asset_management.dto.request;

import com.example.asset_management.dto.enumerator.AssetStatus;
import com.example.asset_management.dto.response.AssetAttributeValueResponse;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AssetFilterRequest(@Nullable String assetCode,
                                 @Nullable String name,
                                 @Nullable String categoryName,
                                 @Nullable AssetStatus assetStatus,
                                 @Nullable List<AssetAttributeValueResponse> assetAttributes) {
}

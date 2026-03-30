package com.example.asset_management.dto.response;

import com.example.asset_management.dto.enumerator.AssetStatus;
import com.example.asset_management.model.Asset;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

public record AssetResponse(Long id, String name,String assetCode, String description, AssetStatus assetStatus,
                            CategoryResponse category, @Nullable Set<ImageResponse> images,
                            @Nullable List<AssetAttributeValueResponse> attributeValues) implements Serializable {

    public static AssetResponse from(Asset asset, CategoryResponse category, Set<ImageResponse> images, List<AssetAttributeValueResponse> attributeValues) {
        return new AssetResponse(asset.getId(), asset.getName(), asset.getAssetCode(),asset.getDescription(), asset.getStatus(),
                category, images, attributeValues);
    }
}

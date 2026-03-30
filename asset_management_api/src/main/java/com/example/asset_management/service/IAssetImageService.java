package com.example.asset_management.service;

import com.example.asset_management.dto.response.ImageResponse;
import com.example.asset_management.model.Asset;
import com.example.asset_management.model.Image;

import java.util.List;
import java.util.Set;

public interface IAssetImageService {

    Set<ImageResponse> insert(Asset asset, List<Image> images);

    void deleteImage(Long assetId, Long imageId);
}

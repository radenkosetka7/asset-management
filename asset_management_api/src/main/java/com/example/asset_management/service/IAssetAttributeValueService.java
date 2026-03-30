package com.example.asset_management.service;

import com.example.asset_management.dto.request.AssetAttributeValueRequest;
import com.example.asset_management.dto.response.AssetAttributeValueResponse;
import com.example.asset_management.model.Asset;

import java.util.List;

public interface IAssetAttributeValueService {

    List<AssetAttributeValueResponse> insert(Asset asset, List<AssetAttributeValueRequest> requests);

    void delete(Long assetId, Long attributeId);
}

package com.example.asset_management.service;

import com.example.asset_management.dto.request.AssetFilterRequest;
import com.example.asset_management.dto.request.AssetRequest;
import com.example.asset_management.dto.response.AssetResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface IAssetService {

    AssetResponse insert(AssetRequest assetRequest, List<MultipartFile> files);

    Optional<AssetResponse> getById(Long assetId);

    Page<AssetResponse> getAll(Pageable page);

    void delete(Long assetId);

    AssetResponse update(Long assetId, AssetRequest assetRequest);

    Page<AssetResponse> getAllByCategoryId(Long categoryId, Pageable page);

    Page<AssetResponse> filterAssets(Pageable page, AssetFilterRequest filterRequest);

    Page<AssetResponse> searchAssets(String query, Pageable page);
}

package com.example.asset_management.repository;

import com.example.asset_management.model.AssetImage;
import com.example.asset_management.model.AssetImageId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetImageRepository extends JpaRepository<AssetImage, AssetImageId> {

    Optional<AssetImage> findByAsset_IdAndImage_Id(Long assetId, Long imageId);

    List<AssetImage> findByAsset_Id(Long assetId);
}

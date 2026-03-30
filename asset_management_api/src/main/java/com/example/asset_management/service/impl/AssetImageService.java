package com.example.asset_management.service.impl;

import com.example.asset_management.dto.response.ImageResponse;
import com.example.asset_management.exception.NotFoundException;
import com.example.asset_management.mapper.AssetDocumentMapper;
import com.example.asset_management.model.*;
import com.example.asset_management.model.elasticsearch.AssetDocument;
import com.example.asset_management.model.elasticsearch.AssetValueDocument;
import com.example.asset_management.repository.AssetAttributeValueRepository;
import com.example.asset_management.repository.AssetDocumentRepository;
import com.example.asset_management.repository.AssetImageRepository;
import com.example.asset_management.repository.AssetRepository;
import com.example.asset_management.service.IAssetImageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AssetImageService implements IAssetImageService {

    private final AssetImageRepository assetImageRepository;
    private final AssetDocumentRepository assetDocumentRepository;
    private final AssetRepository assetRepository;
    private final AssetAttributeValueRepository assetAttributeValueRepository;
    private final AssetDocumentMapper assetDocumentMapper;

    @Override
    public Set<ImageResponse> insert(Asset asset, List<Image> images) {
        log.info("Adding images for asset id: {}", asset.getId());
        Set<ImageResponse> savedImages = new LinkedHashSet<>();
        int sortOrder = 1;
        for (Image image : images) {
            AssetImageId assetImageId = new AssetImageId();
            assetImageId.setAssetId(asset.getId());
            assetImageId.setImageId(image.getId());
            AssetImage assetImage = new AssetImage();
            assetImage.setId(assetImageId);
            assetImage.setAsset(asset);
            assetImage.setImage(image);
            assetImage.setSortOrder(sortOrder++);
            assetImageRepository.save(assetImage);
            savedImages.add(ImageResponse.from(image));
        }
        return savedImages;
    }

    @Override
    @CacheEvict(value = "asset", key = "#assetId")
    public void deleteImage(Long assetId, Long imageId) {
        log.info("Delete image for assetId: {} imageId: {}", assetId, imageId);
        Optional<AssetImage> image = assetImageRepository.findByAsset_IdAndImage_Id(assetId, imageId);
        image.ifPresentOrElse(assetImageRepository::delete, () -> {
            throw new NotFoundException("Image does not exist");
        });
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> {
                    log.error("Asset with id:{} does not exist", assetId);
                    return new NotFoundException("Asset does not exist");
                });

        List<AssetAttributeValue> attributeValues = assetAttributeValueRepository.findByAsset_Id(assetId);
        List<AssetValueDocument> documentValues = assetDocumentMapper.toDocumentEntity(attributeValues);

        assetDocumentRepository.save(
                AssetDocument.from(asset,documentValues)
        );
    }
}

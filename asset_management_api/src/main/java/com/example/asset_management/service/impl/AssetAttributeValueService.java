package com.example.asset_management.service.impl;

import com.example.asset_management.dto.request.AssetAttributeValueRequest;
import com.example.asset_management.dto.response.AssetAttributeValueResponse;
import com.example.asset_management.dto.response.AttributeResponse;
import com.example.asset_management.exception.NotFoundException;
import com.example.asset_management.mapper.AssetDocumentMapper;
import com.example.asset_management.model.Asset;
import com.example.asset_management.model.AssetAttributeValue;
import com.example.asset_management.model.AssetAttributeValueId;
import com.example.asset_management.model.Attribute;
import com.example.asset_management.model.elasticsearch.AssetDocument;
import com.example.asset_management.model.elasticsearch.AssetValueDocument;
import com.example.asset_management.repository.AssetAttributeValueRepository;
import com.example.asset_management.repository.AssetDocumentRepository;
import com.example.asset_management.repository.AssetRepository;
import com.example.asset_management.repository.AttributeRepository;
import com.example.asset_management.service.IAssetAttributeValueService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class AssetAttributeValueService implements IAssetAttributeValueService {

    private final AssetAttributeValueRepository assetAttributeValueRepository;
    private final AttributeRepository attributeRepository;
    private final AssetRepository assetRepository;
    private final AssetDocumentMapper assetDocumentMapper;
    private final AssetDocumentRepository assetDocumentRepository;


    @Override
    public List<AssetAttributeValueResponse> insert(Asset asset, List<AssetAttributeValueRequest> requests) {
        log.info("Inserting attribute values for asset id: {}", asset.getId());
        List<AssetAttributeValueResponse> responses = new ArrayList<>();
        for (AssetAttributeValueRequest request : requests) {
            Attribute attribute = attributeRepository.findById(request.attributeId())
                    .orElseThrow(() -> {
                        log.error("Attribute with id:{} does not exist", request.attributeId());
                        return new NotFoundException("Attribute does not exist");
                    });

            AssetAttributeValueId assetAttributeValueId = new AssetAttributeValueId();
            assetAttributeValueId.setAssetId(asset.getId());
            assetAttributeValueId.setAttributeId(attribute.getId());
            AssetAttributeValue assetAttributeValue = new AssetAttributeValue();
            assetAttributeValue.setId(assetAttributeValueId);
            assetAttributeValue.setAsset(asset);
            assetAttributeValue.setAttribute(attribute);
            assetAttributeValue.setValueString(request.valueString());
            assetAttributeValue.setValueNumber(request.valueNumber());
            assetAttributeValue.setValueDate(request.valueData());
            assetAttributeValue.setValueBool(request.valueBool());
            AssetAttributeValue savedAssetValue = assetAttributeValueRepository.save(assetAttributeValue);
            AttributeResponse attributeResponse = AttributeResponse.from(attribute);
            responses.add(AssetAttributeValueResponse.from(attributeResponse, savedAssetValue));
        }
        return responses;
    }

    @Override
    @CacheEvict(value = "asset", key = "#assetId")
    public void delete(Long assetId, Long attributeId) {
        log.info("Delete attribute for asset: {} attributeId: {}", assetId, attributeId);
        Optional<AssetAttributeValue> assetAttributeValue = assetAttributeValueRepository.findByAsset_IdAndAttribute_Id(assetId, attributeId);
        assetAttributeValue.ifPresentOrElse(assetAttributeValueRepository::delete, () -> {
            throw new NotFoundException("Attribute does not exist");
        });
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> {
                    log.error("Asset with id: {} does not exist", assetId);
                    return new NotFoundException("Asset does not exist");
                });

        List<AssetAttributeValue> attributeValues = assetAttributeValueRepository.findByAsset_Id(assetId);
        List<AssetValueDocument> documentValues = assetDocumentMapper.toDocumentEntity(attributeValues);
        assetDocumentRepository.save(
                AssetDocument.from(asset, documentValues)
        );


    }
}

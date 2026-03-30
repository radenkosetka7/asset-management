package com.example.asset_management.service.impl;

import com.example.asset_management.mapper.AssetDocumentMapper;
import com.example.asset_management.model.Asset;
import com.example.asset_management.model.AssetAttributeValue;
import com.example.asset_management.model.elasticsearch.AssetDocument;
import com.example.asset_management.model.elasticsearch.AssetValueDocument;
import com.example.asset_management.repository.AssetAttributeValueRepository;
import com.example.asset_management.repository.AssetDocumentRepository;
import com.example.asset_management.repository.AssetRepository;
import com.example.asset_management.service.IAssetDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetDocumentService implements IAssetDocumentService {

    private final AssetDocumentRepository assetDocumentRepository;
    private final AssetRepository assetRepository;
    private final AssetAttributeValueRepository assetAttributeValueRepository;

    @Transactional(readOnly = true)
    @Override
    public void reindexAllAssets() {

        int page = 0;
        int size = 500;
        Page<Asset> assetPage;

        do {
            assetPage = assetRepository.findAll(PageRequest.of(page, size));
            List<Asset> assets = assetPage.getContent();
            if (assets.isEmpty()) break;

            List<Long> assetIds = assets.stream()
                    .map(Asset::getId)
                    .toList();

            List<AssetAttributeValue> allValues =
                    assetAttributeValueRepository.findAllWithAttributeByAssetIds(assetIds);

            Map<Long, List<AssetAttributeValue>> valuesByAssetId =
                    allValues.stream()
                            .collect(Collectors.groupingBy(
                                    v -> v.getAsset().getId()
                            ));

            List<AssetDocument> docs = assets.stream()
                    .map(asset -> mapToDocument(asset, valuesByAssetId))
                    .toList();

            assetDocumentRepository.saveAll(docs);

            page++;

        } while (assetPage.hasNext());
    }

    private AssetDocument mapToDocument(
            Asset asset,
            Map<Long, List<AssetAttributeValue>> valuesByAssetId
    ) {

        List<AssetValueDocument> valueDocs =
                valuesByAssetId
                        .getOrDefault(asset.getId(), List.of())
                        .stream()
                        .map(this::mapValue)
                        .toList();

        return AssetDocument.from(asset, valueDocs);
    }

    private AssetValueDocument mapValue(AssetAttributeValue value) {
        AssetValueDocument doc = new AssetValueDocument();

        doc.setAttributeId(value.getAttribute().getId());
        doc.setDataType(value.getAttribute().getDataType().name());
        doc.setValueString(value.getValueString());
        doc.setValueNumber(value.getValueNumber());
        doc.setValueDate(value.getValueDate().atStartOfDay(ZoneId.of("CET")).toInstant().toEpochMilli());
        doc.setValueBool(value.getValueBool());

        return doc;
    }
}

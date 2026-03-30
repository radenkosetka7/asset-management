package com.example.asset_management.service.impl;

import com.example.asset_management.dto.enumerator.AssetStatus;
import com.example.asset_management.dto.request.AssetFilterRequest;
import com.example.asset_management.dto.request.AssetRequest;
import com.example.asset_management.dto.response.*;
import com.example.asset_management.exception.NotFoundException;
import com.example.asset_management.mapper.AssetDocumentMapper;
import com.example.asset_management.model.Asset;
import com.example.asset_management.model.AssetAttributeValue;
import com.example.asset_management.model.Category;
import com.example.asset_management.model.elasticsearch.AssetDocument;
import com.example.asset_management.model.elasticsearch.AssetValueDocument;
import com.example.asset_management.repository.*;
import com.example.asset_management.service.IAssetAttributeValueService;
import com.example.asset_management.service.IAssetService;
import com.example.asset_management.service.IImageService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AssetService implements IAssetService {

    private final CategoryRepository categoryRepository;
    private final AssetRepository assetRepository;
    private final IImageService imageService;
    private final IAssetAttributeValueService attributeValueService;
    private final AssetImageRepository assetImageRepository;
    private final AssetAttributeValueRepository assetAttributeValueRepository;
    private final AssetDocumentRepository assetDocumentRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final AssetDocumentMapper assetDocumentMapper;
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public AssetResponse insert(AssetRequest assetRequest,List<MultipartFile> files) {
        log.info("Adding new asset");
        Category category = categoryRepository.findById(assetRequest.categoryId())
                .orElseThrow(() -> new NotFoundException("Category with id: " + assetRequest.categoryId() + " does not exist"));
        CategoryResponse categoryResponse = CategoryResponse.from(category, null);
        Asset asset = Asset.builder()
                .assetCode(assetRequest.assetCode())
                .name(assetRequest.name())
                .description(assetRequest.description())
                .status(AssetStatus.ACTIVE)
                .createdAt(Instant.now())
                .modifiedAt(Instant.now())
                .category(category)
                .build();

        Asset savedAsset = assetRepository.save(asset);
        Set<ImageResponse> images = imageService.insert(savedAsset, files);
        log.info("New asset has added: {}", savedAsset.getId());
        List<AssetAttributeValueResponse> attributeValueResponses = attributeValueService.insert(asset, assetRequest.assetAttributeValue());
        List<AssetValueDocument> documentValues = assetDocumentMapper.toDocumentDto(attributeValueResponses);
        assetDocumentRepository.save(
                AssetDocument.from(savedAsset, documentValues)
        );
        return AssetResponse.from(asset, categoryResponse, images, attributeValueResponses);
    }

    @Override
    @Cacheable(value = "asset", key = "#assetId")
    public Optional<AssetResponse> getById(Long assetId) {
        log.info("Get asset id: {} ", assetId);
        Set<ImageResponse> assetImages = assetImageRepository.findByAsset_Id(assetId)
                .stream()
                .map(assetImage -> ImageResponse.from(assetImage.getImage()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<AssetAttributeValueResponse> assetAttributeValue = assetAttributeValueRepository
                .findByAsset_Id(assetId)
                .stream()
                .map(assetValue -> AssetAttributeValueResponse.from(AttributeResponse.from(assetValue.getAttribute()), assetValue))
                .toList();
        return assetRepository.findById(assetId)
                .map(asset -> AssetResponse.from(asset, CategoryResponse.from(asset.getCategory()), assetImages, assetAttributeValue));
    }

    @Override
    public Page<AssetResponse> getAll(Pageable page) {
        log.info("Get all assets");
        return assetRepository.findAll(page).map(asset -> {
            Set<ImageResponse> imageResponses = assetImageRepository.findByAsset_Id(asset.getId())
                    .stream()
                    .map(assetImage -> ImageResponse.from(assetImage.getImage()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return AssetResponse.from(asset, CategoryResponse.from(asset.getCategory()), imageResponses, null);
        });
    }

    @Override
    @CacheEvict(value = "asset", key = "#assetId")
    public void delete(Long assetId) {
        log.info("Deleting asset assetId: {}", assetId);
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> {
                    log.error("Asset with id:{} does not exist", assetId);
                    return new NotFoundException("Asset does not exist");
                });

        asset.setStatus(AssetStatus.RETIRED);
        asset.setModifiedAt(Instant.now());
        Asset savedAsset = assetRepository.save(asset);

        List<AssetAttributeValue> attributeValues = assetAttributeValueRepository.findByAsset_Id(assetId);
        List<AssetValueDocument> documentValues = assetDocumentMapper.toDocumentEntity(attributeValues);
        assetDocumentRepository.save(
                AssetDocument.from(savedAsset, documentValues)
        );
    }

    @Override
    @CacheEvict(value = "asset", key = "#assetId")
    public AssetResponse update(Long assetId, AssetRequest assetRequest) {
        log.info("Updating asset assetId: {}", assetId);
        return assetRepository.findById(assetId).map(asset -> {
            asset.setAssetCode(assetRequest.assetCode());
            asset.setName(assetRequest.name());
            asset.setDescription(assetRequest.description());
            if (assetRequest.assetStatus() != null) {
                asset.setStatus(assetRequest.assetStatus());
            }
            Category category = categoryRepository.findById(assetRequest.categoryId())
                    .orElseThrow(() -> new NotFoundException("Category does not exist"));

            asset.setCategory(category);
            asset.setModifiedAt(Instant.now());
            Asset savedAsset = assetRepository.save(asset);

            List<AssetAttributeValue> attributeValues = assetAttributeValueRepository.findByAsset_Id(assetId);
            List<AssetValueDocument> documentValues = assetDocumentMapper.toDocumentEntity(attributeValues);
            assetDocumentRepository.save(
                    AssetDocument.from(savedAsset, documentValues)
            );

            return AssetResponse.from(savedAsset, CategoryResponse.from(savedAsset.getCategory()), null, null);
        }).orElseThrow(() -> new NotFoundException("Asset does not exist"));
    }

    @Override
    public Page<AssetResponse> getAllByCategoryId(Long categoryId, Pageable page) {
        return assetRepository.findAssetByCategory_Id(categoryId, page).map(asset -> {
            Set<ImageResponse> assetImages = assetImageRepository.findByAsset_Id(asset.getId())
                    .stream()
                    .map(assetImage -> ImageResponse.from(assetImage.getImage()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            return AssetResponse.from(asset, CategoryResponse.from(asset.getCategory()), assetImages, null);
        });
    }

    @Override
    public Page<AssetResponse> filterAssets(Pageable page, AssetFilterRequest filterRequest) {

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {
                    if (filterRequest.assetCode() != null) {
                        List<String> tokens = toTokens(filterRequest.assetCode());
                        for (String token : tokens) {
                            b.filter(f -> f.wildcard(w -> w.field("assetCode.keyword")
                                    .value("*" + token + "*")
                                    .caseInsensitive(true)));
                        }
                    }
                    if (filterRequest.name() != null) {
                        List<String> tokens = toTokens(filterRequest.name());
                        for (String token : tokens) {
                            b.filter(f -> f.wildcard(w -> w.field("name.keyword")
                                    .value("*" + token + "*")
                                    .caseInsensitive(true)));
                        }
                    }
                    if (filterRequest.categoryName() != null) {
                        List<String> tokens = toTokens(filterRequest.categoryName());
                        for (String token : tokens) {
                            b.filter(f -> f.wildcard(w -> w.field("categoryName.keyword")
                                    .value("*" + token + "*")
                                    .caseInsensitive(true)));
                        }
                    }
                    if (filterRequest.assetStatus() != null) {
                        b.filter(f -> f.term(t ->
                                t.field("status.keyword").value(filterRequest.assetStatus().name())));
                    }
                    if (filterRequest.assetAttributes() != null) {
                        for (AssetAttributeValueResponse attribute : filterRequest.assetAttributes()) {
                            b.filter(f ->
                                    f.nested(n ->
                                            n.path("attributes")
                                                    .query(nq ->
                                                            nq.bool(nb -> {
                                                                nb.must(m ->
                                                                        m.term(t -> t.field("attributes.attributeId")
                                                                                .value(attribute.attribute().id())));
                                                                switch (attribute.attribute().dataType()) {
                                                                    case STRING -> {
                                                                        List<String> tokens = toTokens(attribute.valueString());
                                                                        for (String token : tokens) {
                                                                            nb.must(m -> m.wildcard(w -> w.field("attributes.valueString.keyword")
                                                                                    .value("*" + token + "*")
                                                                                    .caseInsensitive(true)));
                                                                        }
                                                                    }
                                                                    case NUMBER -> nb.must(m -> m.term(t -> {
                                                                        if (attribute.valueNumber() == null) {
                                                                            throw new IllegalArgumentException("Value number must not be null for NUMBER attribute");
                                                                        }
                                                                        return t.field("attributes.valueNumber")
                                                                                .value(attribute.valueNumber().doubleValue());
                                                                    }));
                                                                    case DATE -> nb.must(m -> m.term(t -> {
                                                                        if (attribute.valueDate() == null) {
                                                                            throw new IllegalArgumentException("Value date must not be null for DATE attribute");
                                                                        }
                                                                        return t.field("attributes.valueDate")
                                                                                .value(attribute.valueDate().atStartOfDay(ZoneOffset.UTC)
                                                                                        .toInstant().toEpochMilli());
                                                                    }));
                                                                    case BOOLEAN ->
                                                                            nb.must(m -> m.term(t -> t.field("attributes.valueBool")
                                                                                    .value(Boolean.TRUE.equals(attribute.valueBool()))));
                                                                }
                                                                return nb;
                                                            }))
                                    ));
                        }
                    }
                    return b;
                }))
                .withPageable(page)
                .withSort(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
                .build();

        SearchHits<AssetDocument> hits =
                elasticsearchOperations.search(query, AssetDocument.class);

        List<Long> ids = hits.getSearchHits().stream()
                .map(h -> h.getContent().getId())
                .toList();

        return getAssetResponses(page, hits, ids);
    }

    private Page<AssetResponse> getAssetResponses(Pageable page, SearchHits<AssetDocument> hits, List<Long> ids) {

        if (ids.isEmpty()) {
            return Page.empty(page);
        }
        List<AssetResponse> assets = assetRepository.findByIdIn(ids).stream()
                .map(asset -> {
                    Set<ImageResponse> assetImages = assetImageRepository.findByAsset_Id(asset.getId())
                            .stream()
                            .map(assetImage -> ImageResponse.from(assetImage.getImage()))
                            .collect(Collectors.toCollection(LinkedHashSet::new));
                    return AssetResponse.from(asset, CategoryResponse.from(asset.getCategory()), assetImages, null);

                }).toList();

        Map<Long, AssetResponse> map = assets.stream()
                .collect(Collectors.toMap(AssetResponse::id, a -> a));

        List<AssetResponse> ordered = ids.stream()
                .map(map::get)
                .filter(Objects::nonNull)
                .toList();

        return new PageImpl<>(
                ordered,
                page,
                hits.getTotalHits()
        );
    }

    @Override
    public Page<AssetResponse> searchAssets(String query, Pageable page) {
        String trimmedQuery = query == null ? "" : query.trim();
        if (trimmedQuery.isEmpty()) {
            return Page.empty(page);
        }

        NativeQuery searchQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        // Keep exact/prefix code matching strong for technical identifiers.
                        .should(s -> s.term(t -> t.field("assetCode.keyword").value(trimmedQuery).boost(8.0F)))
                        .should(s -> s.prefix(p -> p.field("assetCode.keyword").value(trimmedQuery).caseInsensitive(true).boost(5.0F)))
                        // Human text search across analyzed text fields.
                        .should(s -> s.match(m -> m.field("name").query(trimmedQuery).boost(4.0F)))
                        .should(s -> s.matchPhrasePrefix(mpp -> mpp.field("name").query(trimmedQuery).boost(3.0F)))
                        .should(s -> s.match(m -> m.field("categoryName").query(trimmedQuery).boost(1.5F)))
                        .minimumShouldMatch("1")
                ))
                .withPageable(page)
                .withSort(Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))
                .build();

        SearchHits<AssetDocument> hits = elasticsearchOperations.search(searchQuery, AssetDocument.class);
        List<Long> assetIds = hits.getSearchHits().stream()
                .map(h -> h.getContent().getId())
                .toList();

        return getAssetResponses(page, hits, assetIds);

    }

    private List<String> toTokens(String value) {
        if (value == null) {
            return List.of();
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(trimmed.split("\\s+")).filter(token -> !token.isBlank()).toList();
    }
}

package com.example.asset_management.controller;

import com.example.asset_management.dto.request.AssetFilterRequest;
import com.example.asset_management.dto.request.AssetRequest;
import com.example.asset_management.dto.response.AssetResponse;
import com.example.asset_management.service.IAssetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/assets")
@Validated
@RequiredArgsConstructor
public class AssetController {

    private final IAssetService assetService;
    private final ObjectMapper objectMapper;

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public AssetResponse insert(
            @RequestPart("asset") String assetJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> files) {
        AssetRequest assetRequest = parseAssetRequest(assetJson);
        return assetService.insert(assetRequest, files);
    }

    private AssetRequest parseAssetRequest(String assetJson) {
        if (assetJson == null || assetJson.isBlank()) {
            throw new IllegalArgumentException("Asset payload must not be empty");
        }
        try {
            return objectMapper.readValue(assetJson, AssetRequest.class);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid asset payload", ex);
        }
    }

    @GetMapping
    public HttpEntity<PagedModel<EntityModel<AssetResponse>>> getAll(
            Pageable pageable,
            PagedResourcesAssembler<AssetResponse> assembler) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        Page<AssetResponse> assets = assetService.getAll(sortedPageable);
        return ResponseEntity.ok(assembler.toModel(assets));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetResponse> getById(@PathVariable Long id) {
        return assetService.getById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assetService.delete(id);
        return ResponseEntity.noContent().build();

    }

    @PutMapping("/{id}")
    public ResponseEntity<AssetResponse> update(@PathVariable Long id, @RequestBody AssetRequest assetRequest) {
        return ResponseEntity.ok(assetService.update(id, assetRequest));
    }

    @GetMapping("/categories/{id}")
    public HttpEntity<PagedModel<EntityModel<AssetResponse>>> getAllByCategoryId(@PathVariable Long id, Pageable pageable, PagedResourcesAssembler<AssetResponse> assembler) {
        Page<AssetResponse> assets = assetService.getAllByCategoryId(id, pageable);
        return ResponseEntity.ok(assembler.toModel(assets));
    }

    @PostMapping("/filter")
    public HttpEntity<PagedModel<EntityModel<AssetResponse>>> filterAssets(Pageable page, @RequestBody AssetFilterRequest filterRequest, PagedResourcesAssembler<AssetResponse> assembler) {
        Page<AssetResponse> assets = assetService.filterAssets(page, filterRequest);
        return ResponseEntity.ok(assembler.toModel(assets));
    }

    @GetMapping("/search")
    public HttpEntity<PagedModel<EntityModel<AssetResponse>>> searchAssets(
            @RequestParam
            @NotBlank(message = "Query must not be empty")
            @Size(max = 100, message = "Query too long")
            String query,
            Pageable page,
            PagedResourcesAssembler<AssetResponse> assembler) {
        Page<AssetResponse> assets = assetService.searchAssets(query, page);
        return ResponseEntity.ok(assembler.toModel(assets));

    }
}
